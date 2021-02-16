# 개인 시나리오

### 기능적 요구사항
1. 고객이 결제하면 포인트가 적립된다. 
2. 결제가 취소되면 포인트 적립이 취소된다. 

### 비기능적 요구사항
1. 트랜잭션
    1. 결제 취소 시 포인트 적립도 취소되어야 한다. (Sync)
1. 장애격리
    1. 포인트 적립 서비스에 문제가 있어도 결제는 정상적으로 이루어져야 한다. (Async, Eventual Consistency)
	1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시 후에 하도록 유도한다. (Circuit breaker, fallback)
1. 성능
    1. 포인트 적립 내역은 SirenOrderHome에서 언제든지 조회할 수 있다. (CQRS)

 
# Event Storming 결과

![image](https://user-images.githubusercontent.com/74236548/108019495-b3752380-705d-11eb-8349-282b298e9fc9.png)

# 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/74236548/108019542-cf78c500-705d-11eb-9981-a69a83f848af.png)


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085, 8088 이다)
```
cd SirenOrder
mvn spring-boot:run  

cd Payment
mvn spring-boot:run

cd SirenOrderHome
mvn spring-boot:run 

cd Shop
mvn spring-boot:run  

cd gateway
mvn spring-boot:run  

cd Point
mvn spring-boot:run  
```

## DDD 의 적용
msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.

Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

**Point 서비스의 Point.java **

```java 
package winterschoolone;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Point_table")
public class Point {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String userId;
    private Integer point;
    

    @PrePersist
    public void onPrePersist(){
        PointSaved pointSaved = new PointSaved();
        BeanUtils.copyProperties(this, pointSaved);
        pointSaved.publishAfterCommit();


    }

    @PreUpdate
    public void onPreUpdate(){
        PointCancelled pointCancelled = new PointCancelled();
        BeanUtils.copyProperties(this, pointCancelled);
        pointCancelled.publishAfterCommit();


    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }
}
```

**Point 서비스의 PolicyHandler.java **
```java
package winterschoolone;

import winterschoolone.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    PointRepository pointRepository;
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayed_(@Payload Payed payed){

        if(payed.isMe()){
            System.out.println("##### listener_wheneverPayed_Point  : " + payed.toJson());
            
            Point point = new Point();
            point.setOrderId(payed.getOrderId());
            point.setUserId(payed.getUserId());
            point.setPoint(payed.getQty()*10);
            
            pointRepository.save(point);
        }
    }
}
```

- DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.  
  
- 주문(결제) 후 Point 적립 결과 

![image](https://user-images.githubusercontent.com/74236548/108023339-a78d5f80-7065-11eb-9c66-0be70841c165.png)

# GateWay 적용
API GateWay를 통하여 마이크로 서비스들의 집입점을 통일할 수 있다.
다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: SirenOrder
          uri: http://localhost:8081
          predicates:
            - Path=/sirenOrders/** 
        - id: Payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: Shop
          uri: http://localhost:8083
          predicates:
            - Path=/shops/** 
        - id: SirenOrderHome
          uri: http://localhost:8084
          predicates:
            - Path= /sirenOrderHomes/**
        - id: Point
          uri: http://localhost:8085
          predicates:
            - Path= /points/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: SirenOrder
          uri: http://SirenOrder:8080
          predicates:
            - Path=/sirenOrders/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: Shop
          uri: http://Shop:8080
          predicates:
            - Path=/shops/** 
        - id: SirenOrderHome
          uri: http://SirenOrderHome:8080
          predicates:
            - Path= /sirenOrderHomes/**
        - id: Point
          uri: http://Point:8080
          predicates:
            - Path= /points/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080


```

# CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서 View 역할은 SirenOrderHomes 서비스가 수행한다.

- 포인트 적립 후 SirenOrderHomes 화면

![image](https://user-images.githubusercontent.com/74236548/108023397-c986e200-7065-11eb-9637-eee9139f9778.png)

- 포인트 적립 취소 후 SirenOrderHomes 화면

![image](https://user-images.githubusercontent.com/74236548/108023409-cc81d280-7065-11eb-90c1-141c7178ecc5.png)

또한 Correlation을 key를 활용하여 orderId를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위와 같이 주문을 하게되면 SirenOrder -> Payment -> Point 적립 되고

주문 취소가 되면 Point가 0으로 Update 되는 것을 볼 수 있다.

또한 Correlation을 key를 활용하여 orderId를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

# 폴리글랏

Shop 서비스의 DB와 SirenOrder의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Shop의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/74236548/108023573-25516b00-7066-11eb-8461-f371354d95a2.png)

**Point의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/74236548/108023598-3b5f2b80-7066-11eb-81c5-c5328acfc547.png)

# 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 주문(SirenOrder)->결제(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

**Payment 서비스 내 external.PointService **
```java
package winterschoolone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Point", url="api.url.Payment")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points")
    public void point(@RequestBody Point point);

}
```

**동작 확인**
- Point 서비스 중지 후 결제 취소 

![image](https://user-images.githubusercontent.com/74236548/108028721-9fd2b880-706f-11eb-80bb-681bc3b45225.png)


- Point 서비스 재기동 후 정상 취소 (point : 0점) 확인 

![image](https://user-images.githubusercontent.com/74236548/108029032-1f608780-7070-11eb-9bc3-1284c08f6838.png)

![image](https://user-images.githubusercontent.com/74236548/108029080-369f7500-7070-11eb-959a-651fa103511e.png)


