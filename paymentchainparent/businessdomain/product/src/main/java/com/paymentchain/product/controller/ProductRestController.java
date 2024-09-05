/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.paymentchain.product.controller;

import com.paymentchain.product.entities.Product;
import com.paymentchain.product.exceptions.customExceptions.BussinesRuleException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import com.paymentchain.product.respository.ProductRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 *
 * @author sotobotero
 */
@RestController
@RequestMapping("/product")
public class ProductRestController {
    
    @Autowired
    ProductRepository productRepository;
    
    @GetMapping()
    public ResponseEntity<List<Product>> list() {
        List<Product> findAll = productRepository.findAll();
        
        if(findAll.isEmpty()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        
        return ResponseEntity.ok(findAll);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Optional<Product>> get(@PathVariable(name = "id") long id) {
         Optional<Product> findById = productRepository.findById(id);
        if(findById.isPresent()){
            return ResponseEntity.ok(findById);
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable(name = "id") long id, @RequestBody Product input) throws BussinesRuleException {
        Optional exists = productRepository.findById(id);   

        if(exists.isEmpty()){
            throw new BussinesRuleException("No product with ID " + id + " found", HttpStatus.NOT_FOUND);
        }
        
        Product find = (Product) exists.get();
        find.setCode(input.getCode());
        find.setName(input.getName());

        Product save = productRepository.save(find);
        return ResponseEntity.ok(save);
    }
    
    @PostMapping
    public ResponseEntity<?> post(@RequestBody Product input) throws BussinesRuleException {
        
        if(input.getCode()== null || input.getCode().trim().isBlank()){
            throw new BussinesRuleException("A product must have a code", HttpStatus.BAD_REQUEST);
        }
        
        if(input.getName()== null || input.getName().trim().isBlank()){
            throw new BussinesRuleException("A product must have a name", HttpStatus.BAD_REQUEST);
        }
        
        Optional alreadyExists = productRepository.findById(input.getId());
        
        if(alreadyExists.isPresent()){
            throw new BussinesRuleException("A product with ID " + input.getCode() + " already exists", HttpStatus.PRECONDITION_FAILED);
        }
        
        Product save = productRepository.save(input);
        

        
        return ResponseEntity.ok(save);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable(name = "id") long id) throws BussinesRuleException {  
        Optional<Product> findById = productRepository.findById(id);   
        
        if(findById.isEmpty()){
            throw new BussinesRuleException("No product with ID " + id + " found", HttpStatus.NOT_FOUND);
        }
 
        try {
            productRepository.delete(findById.get());  
        } catch(IllegalArgumentException ex){
            throw new BussinesRuleException("ID is null", HttpStatus.BAD_REQUEST);
        }
        
        return ResponseEntity.ok().build();
    }
    
}
