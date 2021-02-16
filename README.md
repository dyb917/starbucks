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

**Point 서비스의 Point.java**


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

**Point 서비스의 PolicyHandler.java**

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

**Payment 서비스 내 external.PointService**
```java
package winterschoolone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Point", url="api.url.Point")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points")
    public void point(@RequestBody Point point);

}

```

**동작 확인**
- Point 서비스 중지 후 결제 취소 

![image](https://user-images.githubusercontent.com/74236548/108028721-9fd2b880-706f-11eb-80bb-681bc3b45225.png)


- Point 서비스 재기동 후 정상 취소 (point : 0점) 확인 

![image](https://user-images.githubusercontent.com/74236548/108030091-ee815200-7071-11eb-92e0-6617016a0094.png)


# 운영

# Deploy / Pipeline

- git에서 소스 가져오기
```
git clone https://github.com/dyb917/starbucks.git
```
- Build 하기
```
cd /starbucks
cd gateway
mvn package

cd ..
cd sirenorder
mvn package

cd ..
cd payment
mvn package

cd ..
cd shop
mvn package

cd ..
cd sirenorderhome
mvn package

cd ..
cd Point
mvn package

```

- Docker Image Push/deploy/서비스생성
```
kubectl create ns tutorial

cd gateway
az acr build --registry skuser08 --image skuser08.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skuser08.azurecr.io/gateway:v1 -n tutorial
kubectl expose deploy gateway --type=ClusterIP --port=8080 -n tutorial

cd ..
cd payment
az acr build --registry skuser08 --image skuser08.azurecr.io/payment:v1 .
kubectl create deploy payment --image=skuser08.azurecr.io/payment:v1 -n tutorial
kubectl expose deploy payment --type=ClusterIP --port=8080 -n tutorial

cd ..
cd shop
az acr build --registry skuser08 --image skuser08.azurecr.io/sirenorderhome:v1 .
kubectl create deploy shop --image=skuser08.azurecr.io/sirenorderhome:v1 -n tutorial
kubectl expose deploy shop --type=ClusterIP --port=8080 -n tutorial

cd ..
cd sirenorderhome
az acr build --registry skuser08 --image skuser08.azurecr.io/sirenorderhome:v1 .
kubectl create deploy sirenorderhome --image=skuser08.azurecr.io/sirenorderhome:v1 -n tutorial
kubectl expose deploy sirenorderhome --type=ClusterIP --port=8080 -n tutorial

cd ..
cd sirenorder
az acr build --registry skuser08 --image skuser08.azurecr.io/sirenorder:v1 .
kubectl create deploy sirenorder --image=skuser08.azurecr.io/sirenorder:v1 -n tutorial
kubectl expose deploy sirenorder --type=ClusterIP --port=8080 -n tutorial
cd


```

- yml파일 이용한 deploy
```
cd ..
cd Point
az acr build --registry skuser08 --image skuser08.azurecr.io/point:v1 .

kubectl apply -f ./deployment.yml
kubectl expose deploy point --type=ClusterIP --port=8080 -n tutorial

```

![image](https://user-images.githubusercontent.com/74236548/108037304-c4815d00-707c-11eb-9354-089c0b32f9a6.png)

![image](https://user-images.githubusercontent.com/74236548/108037373-dfec6800-707c-11eb-90e2-6a636ec4087d.png)


- winterone/Point/kubernetes/deployment.yml 파일 
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: point
  namespace: tutorial
  labels:
    app: point
spec:
  replicas: 1
  selector:
    matchLabels:
      app: point
  template:
    metadata:
      labels:
        app: point
    spec:
      containers:
        - name: point
          image: hispres.azurecr.io/point:v1
          ports:
            - containerPort: 8080
          env:
            - name: configurl
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url
```	  
- deploy 완료

![image](https://user-images.githubusercontent.com/74236548/108043173-20031900-7084-11eb-9e60-9ccb134eb6f9.png)

# ConfigMap 
- 시스템별로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리

- application.yml 파일에 ${configurl} 설정

```yaml
      feign:
        hystrix:
          enabled: true
      hystrix:
        command:
          default:
            execution.isolation.thread.timeoutInMilliseconds: 610
      api:
        url:
          Payment: ${configurl}

```

- ConfigMap 사용(\Payment\src\main\java\winterschoolone\external\PointService.java) 

```java

    @FeignClient(name="Point", url="${api.url.Point}")
	public interface PointService {

		@RequestMapping(method= RequestMethod.POST, path="/points")
		public void point(@RequestBody Point point);

	}
```

- Deployment.yml 에 ConfigMap 적용

![image](https://user-images.githubusercontent.com/74236548/107925407-c2a19600-6fb7-11eb-9325-6bd2cd94455c.png)

- ConfigMap 생성

```
kubectl create configmap apiurl --from-literal=url=http://10.0.92.205:8080 -n tutorial

```

![image](https://user-images.githubusercontent.com/74236548/108044732-fa770f00-7085-11eb-890c-ff1a9765518e.png)

  
# 오토스케일 아웃

- 서킷 브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만, 사용자의 요청이 급증하는 경우, 오토스케일 아웃이 필요하다.

>- 단, 부하가 제대로 걸리기 위해서, recipe 서비스의 리소스를 줄여서 재배포한다.(skuser08/starbucks/Point/kubernetes/deployment.yml 수정)

```yaml
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- 다시 expose 해준다.
```
kubectl expose deploy point --type=ClusterIP --port=8080 -n tutorial
```
- recipe 시스템에 replica를 자동으로 늘려줄 수 있도록 HPA를 설정한다. 설정은 CPU 사용량이 15%를 넘어서면 replica를 10개까지 늘려준다.
```
kubectl autoscale deploy point --min=1 --max=10 --cpu-percent=15 -n tutorial
```
- siege를 활용해서 워크로드를 1분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it pod/siege -c siege -n tutorial -- /bin/bash
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://10.0.180.165:8080/payments POST {"userId": "user10", "menuId": "menu10", "qty":10, "orderId":1}'

```
![image](https://user-images.githubusercontent.com/74236548/108061210-55673100-709b-11eb-8d4a-82375778bb24.png)

- 오토스케일 모니터링을 걸어 스케일 아웃이 자동으로 진행됨을 확인한다.
```
kubectl get all -n tutorial
```

![image](https://user-images.githubusercontent.com/74236548/108060706-9874d480-709a-11eb-9f26-d164750f76e6.png)


# 서킷 브레이킹

- 서킷 브레이킹 프레임워크의 선택 : Spring FeignClient + Hystrix 옵션을 사용하여 구현함
- Hystrix를 설정 : 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도
  유지되면 CB 회로가 닫히도록(요청을 빠르게 실패처리, 차단) 설정

- 동기 호출 주체인 Payment에서 Hystrix 설정 
- Payment/src/main/resources/application.yml 파일
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 부하에 대한 지연시간 발생코드
- starbucks/Point/src/main/java/winterschoolone/Point.java

``` java

    @PrePersist
    public void onPrePersist(){
        PointSaved pointSaved = new PointSaved();
        BeanUtils.copyProperties(this, pointSaved);
        pointSaved.publishAfterCommit();

        try {
                Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
                e.printStackTrace();
        }
 
    }
```

- 부하 테스터 siege툴을 통한 서킷 브레이커 동작확인 :
  
  동시 사용자 100명, 60초 동안 실시 
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://10.0.14.180:8080/sirenOrders 
POST {"userId": "user10", "menuId": "menu10", "qty":10}'

siege -c100 -t60S -r10 -v --content-type "application/json" 'http://10.0.180.165:8080/payments POST {"userId": "user10", "menuId": "menu10", "qty":10, "orderId":1}'

```
- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 다시 처리되면서 SirenOrders를 받기 시작

![증빙10](https://user-images.githubusercontent.com/77368578/107917672-a8fa5180-6fab-11eb-9864-69af16a94e5e.png)

# 무정지 배포

- 무정지 배포가 되지 않는 readiness 옵션을 제거 설정
winterone/Point/kubernetes/deployment_n_readiness.yml
```yml
        spec:
      containers:
        - name: point
          image: skuser08.azurecr.io/point:v1
          ports:
            - containerPort: 8080
#          readinessProbe:
#            httpGet:
#              path: '/actuator/health'
#              port: 8080
#            initialDelaySeconds: 10
#            timeoutSeconds: 2
#            periodSeconds: 5
#            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
- 무정지 배포가 되지 않아 Siege 결과 Availability가 100%가 되지 않음

![image](https://user-images.githubusercontent.com/74236548/108084965-2ad7a100-70b8-11eb-9427-ec3b828cdad1.png)
![무정지배포(readiness 제외) 실행결과](https://user-images.githubusercontent.com/77368578/108004276-c295aa80-7038-11eb-9618-1c85fe0a2f53.png)

- 무정지 배포를 위한 readiness 옵션 설정
winterone/Point/kubernetes/deployment.yml
```yml
        spec:
      containers:
        - name: point
          image: skuser08.azurecr.io/point:v1
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- 무정지 배포를 위한 readiness 옵션 설정 후 적용 시 Siege 결과 Availability가 100% 확인

![image](https://user-images.githubusercontent.com/74236548/108074473-93b91c00-70ac-11eb-915b-22b9ea41ce50.png)
![image](https://user-images.githubusercontent.com/74236548/108074252-56548e80-70ac-11eb-8fc8-21abd30a19c9.png)

# Self-healing (Liveness Probe)

- Self-healing 확인을 위한 Liveness Probe 옵션 변경
winterone/Point/kubernetes/deployment_live.yml
```yml
    spec:
      containers:
        - name: point
          image: skuser08.azurecr.io/point:v1
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8081
            initialDelaySeconds: 5
            periodSeconds: 5
```

- Point pod에 Liveness Probe 옵션 적용 확인

![image](https://user-images.githubusercontent.com/74236548/108082964-f06d0480-70b5-11eb-83cc-49f4db6a1bde.png)


- Point pod에서 적용 시 retry발생 확인

![image](https://user-images.githubusercontent.com/74236548/108081733-a0417280-70b4-11eb-8c3b-dfdf2ca85478.png)

