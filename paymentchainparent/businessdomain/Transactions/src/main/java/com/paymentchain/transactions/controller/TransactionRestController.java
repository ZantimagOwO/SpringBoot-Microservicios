/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.paymentchain.transactions.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentchain.transactions.entities.Transaction;
import com.paymentchain.transactions.exceptions.customExceptions.ApiConnectionException;
import com.paymentchain.transactions.exceptions.customExceptions.BusinessRuleException;
import com.paymentchain.transactions.respository.TransactionRepository;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 *
 * @author sotobotero
 */
@RestController
@RequestMapping("/transaction")
public class TransactionRestController {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    HttpClient client = HttpClient.create()
            //Connection Timeout: is a period within which a connection between a client and a server must be established
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPIDLE, 300)
            .option(EpollChannelOption.TCP_KEEPINTVL, 60)
            //Response Timeout: The maximun time we wait to receive a response after sending a request
            .responseTimeout(Duration.ofSeconds(1))
            // Read and Write Timeout: A read timeout occurs when no data was read within a certain 
            //period of time, while the write timeout when a write operation cannot finish at a specific time
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                connection.addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS));
            });

    @GetMapping()
    public ResponseEntity<List<Transaction>> list() {

        List<Transaction> findAll = transactionRepository.findAll();

        if (findAll.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(findAll);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable(name = "id") long id) {
        return transactionRepository.findById(id).map(x -> ResponseEntity.ok(x)).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/transactions")
    public ResponseEntity<List<Transaction>> get(@RequestParam(name = "ibanAccount") String ibanAccount) {

        List<Transaction> findByIbanAccount = transactionRepository.findByIbanAccount(ibanAccount);
        if (findByIbanAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(findByIbanAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable(name = "id") long id, @RequestBody Transaction input) throws BusinessRuleException {
        Optional<Transaction> exists = transactionRepository.findById(id);

        if (exists.isEmpty()) {
            throw new BusinessRuleException("No product with ID " + id + " found", HttpStatus.NOT_FOUND);
        }

        Transaction find = (Transaction) exists.get();
        find.setAmount(input.getAmount());
        find.setChannel(input.getChannel());
        find.setDate(input.getDate());
        find.setDescription(input.getDescription());
        find.setFee(input.getFee());
        find.setIbanAccount(input.getIbanAccount());
        find.setReference(input.getReference());
        find.setStatus(input.getStatus());

        Transaction save = transactionRepository.save(find);
        return ResponseEntity.ok(save);
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody Transaction input) throws BusinessRuleException, UnknownHostException {

        if (input.getIbanAccount() == null || input.getIbanAccount().trim().isBlank()) {
            throw new BusinessRuleException("A transaction must have an IBAN asociated", HttpStatus.BAD_REQUEST);
        }

        if (input.getReference() == null || input.getReference().trim().isBlank()) {
            throw new BusinessRuleException("A transaction must have a reference", HttpStatus.BAD_REQUEST);
        }
        
        //ibanExists(input.getIbanAccount());
        
        input.calcularMonto();
        double dineroEnCuenta = calcularDineroEnCuenta(input.getIbanAccount());
        
        if(dineroEnCuenta + input.getAmount() <= 0){
            throw new BusinessRuleException("Not enought money, transaction canceled", HttpStatus.PRECONDITION_FAILED);
        }

        Transaction save = transactionRepository.save(input);

        return ResponseEntity.ok(save);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable(name = "id") long id) throws BusinessRuleException {
        Optional<Transaction> findById = transactionRepository.findById(id);
        if (findById.isEmpty()) {
            throw new BusinessRuleException("No product with ID " + id + " found", HttpStatus.NOT_FOUND);
        }

        try {
            transactionRepository.delete(findById.get());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("ID is null", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok().build();
    }

    private boolean ibanExists(String iban) throws UnknownHostException {

        try {
            WebClient build = webClientBuilder.clientConnector(new ReactorClientHttpConnector(client))
                    .baseUrl("http://BUSINESSDOMAIN-CUSTOMER/customer")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultUriVariables(Collections.singletonMap("url", "http://BUSINESSDOMAIN-CUSTOMER/customer"))
                    .build();
            build.method(HttpMethod.GET).uri("/iban/" + iban)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new BusinessRuleException("No account associated with that IBAN", HttpStatus.PRECONDITION_FAILED));
                        }
                        return Mono.error(new ApiConnectionException("Could not connect to customer service", HttpStatus.BAD_GATEWAY));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new RuntimeException("Error in customer service")))
                    .bodyToMono(JsonNode.class).block();
            return true;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            } else {
                throw new UnknownHostException(ex.getMessage());
            }
        }
    }
    
    private double calcularDineroEnCuenta(String iban){
        List<Transaction> transactions = transactionRepository.findByIbanAccount(iban);
        double total = 0;
        for(Transaction t : transactions){
            total += t.getAmount();
        }
        return total;
    }

}
