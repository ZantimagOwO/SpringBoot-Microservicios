start "Eureka" java -jar "C:\Udemy\SpringBoot Microservicios\paymentchainparent\infraestructuradomain\eurekaserver\target\eurekaServer-0.0.1-SNAPSHOT.jar"
start "SBAdmin" java -jar "C:\Udemy\SpringBoot Microservicios\paymentchainparent\infraestructuradomain\springBotAdmin\target\SpringBootAdmin-0.0.1-SNAPSHOT.jar"
start "Billing" java -jar "C:\Udemy\SpringBoot Microservicios\billing\target\billing-0.0.1-SNAPSHOT.jar" 
start "Customer" java -jar "C:\Udemy\SpringBoot Microservicios\paymentchainparent\businessdomain\customer\target\customer-0.0.1-SNAPSHOT.jar" 
start "Product" java -jar "C:\Udemy\SpringBoot Microservicios\paymentchainparent\businessdomain\product\target\product-0.0.1-SNAPSHOT.jar" 
start "Transactions" java -jar "C:\Udemy\SpringBoot Microservicios\paymentchainparent\businessdomain\transactions\target\transactions-0.0.1-SNAPSHOT.jar" 

start chrome http://localhost:8081/swagger-ui/index.html#
start chrome http://localhost:8082/swagger-ui/index.html#
start chrome http://localhost:8083/swagger-ui/index.html#
start chrome http://localhost:8084/swagger-ui/index.html#
start chrome http://localhost:8761/