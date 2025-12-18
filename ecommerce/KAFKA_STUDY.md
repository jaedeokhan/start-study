# KAFKA 기본 개념 학습  및 설치

## KAFKA 주요 용어 간단 정리
* topic: 메시지를 논리적으로 분류,저장하는 단위
  * Ex) 회원 가입 완료
* partition: 토픽의 메시지를 분할 저장하는 단위
  * 토픽의 병렬처리와 확장성을 위해 분할한 단위로, 메시지 순서 보장 최소 저장 단위
* message: 파티션 별로 쌓이는 처리해야 하는 데이터
  * 토픽으로 전송하는 데이터 레코드, key-value(및 헤더)로 구성된 이벤트 정보
* broker: 토픽의 메시지를 저장,관리 요청을 처리하는 서버
  * Controller(Leader 재분배), Coordinator(Rebalance) 역할 수행
* producer: 브로커에게 토픽의 메시지를 생성해 토픽을 생산(발행)하는 서비스 (publish)
* consumer: 브로커에게 토픽의 메시지를 소비(구독)하는 서비스 (subscribe)
* consumer group: 각각의 메시지를 처리하는 하나의 집합
* offset: 컨슈머가 어디까지 처리했는지 나타내는 offset
  * 동일 메시지를 재처리하지 않고, 처리하지 않은 메시지를 건너뛰지 않기 위해 마지막까지 처리한 offset을 저장(커밋)해야함


## Broker
* 프로듀서의 메시지를 받아 offset 지정 후 디스크에 저장
* 컨슈머의 파티션 Read에 응답해 디스크의 메시지 전송
* Cluster 내에 각 1개씩 존재하는 Role Broker 역할 수행
  * Controller - 브로커 Leader 재분배
    * 브로커는 Leader, Follower 구조가 중요하다.
    * Leader만 컨슈머와 프로듀서와 소통하고 모든 읽기와 쓰기를 전담한다.
    * Follwer에서는 복제만 하다가 Leader가 다운되면 승격한다.
  * Coordinator - 컨슈머 Rebalance
    * 컨슈머 그룹 모니터링 및 컨슈머 장애 시 특정 파티션을 컨슈머에 매칭

## Kafka + Zookeeper vs KRaft(Kafka Raft)

### Zookeeper
Zookeeper는 카프카 브로커들 외부에서 감시하고 정보를 저장하는 외부 관리 시스템(현재는 사라지는 추세이다.) 

### KRaft
Kafka 최근에는 Zookeeper를 표준으로는 사용하지 않고,
메타 데이터 관리를 Zookeepr가 아닌 브로커 내부로 기능을 흡수하는 형식이 KRaft이다.

## Topic, Partition

### Topic
Topic은 메시지를 분류하는 기준이며 N개의 Partition으로 구성

### Topic 네이밍
Topic의 네이밍은 하단과 같이 지을려고 한다.
현재는 환경별로 구분자는 생략할려고 한다.

* 주문 생성 : ecommerce.order.event.created.v1
* 쿠폰 발급 : ecommerce.coupon.event.issued.v1

1. 서비스: 각 서비스 이름 (ecommerce, etc)
2. 도메인 (Domain): 업무 영역 또는 담당 팀	order (주문), pay (결제), item (상품)
3. 타입 (Type): 메시지의 성격 (가장 중요한 부분!)	event (사건), cmd (명령), log (로그)
4. 설명 (Description): 구체적으로 어떤 행위인지	created, shipped, refund_requested
5. 버전 (Version): 데이터 구조(스키마) 변경 대응	v1, v2

### Partition
* 파티션에 발행된 순서대로 소비(구독)함으로써 순차처리 보장
  * 대용량 트래픽을 파티션의 개수만큼 병렬로 처리하기에 빠른 처리 가능
* 한 파티션은 하나의 컨슈머에서만 컨슘할 수 있음
  * 1 컨슈머 : N 파티션 = 가능 ==> 컨슈머가 바쁨
  * N 컨슈머 : 1 파티션 = 불가능 ==> 같은 그룹 내에서는 절대 안 된다.
  * `중요, 하나의 컨슈머는 여러개의 파티션 접근 가능`
  * 파티션을 쪼개서 N개의 컨슈머에 붙을 수는 없지만, 컨슈머는 N개의 파티션에 접근 가능 
  * Ex) 파티션 3개 vs 컨슈머 1개일 때, 컨슈머가 1개가 3개의 파티션을 처리 가능하지만 `Lag 발생 가능성 존재`
    ```
    [파티션 0] ↘
    [파티션 1] → [컨슈머 A]  (혼자서 3개 다 처리)
    [파티션 2] ↗
    ```
  * Ex) 파티션 3개 vs 컨슈머 3개일 때, 가장 이상적
  * Ex) 파티션 3개 vs 컨슈머 5개일 때, 낭비 발생한다. 가능하지만 2개의 컨슈머는 놀고있다.

### 하나의 파티션에 여러개의 컨슈머가 붙을 수 없는 이유는?
가장 중요한 이유는 순서(Order) 보장 때문이다.
파티션은 큐와 같기에 데이터가 들어온 순서대로 쌓인다.

## KRaft 설치(wsl2)

### 설치 시 에러 정리
1. bitnami/kafka 사용 시 latest 버전 및 잘못 명시 에러
   * bitnami 대신 confluentinc 사용 변경
```
> docker-compose up -d
Error response from daemon: manifest for bitnami/kafka:latest not found: manifest unknown: manifest unknown
```

2. KRaft 설치 시 yml 들여쓰기 에러 
   * kafka-ui부분 들여쓰기 문제
```
> docker-compose up -d
yaml: line 18: did not find expected key
```

### 설치 진행

| 분류            | 값 | 설명                   |
|---------------|--|----------------------|
| KAFKA_NODE_ID | 1 | KAFKA 노드의 고유 식별자     |
| KAFKA_PROCESS_ROLES | broker,controller | 브로커, 컨트롤러 역할 지정      |
| KAFKA_CONTROLLER_QUORUM_VOTERS | 1@kafka:29093 | KRaft 컨트롤러 투표자 설정    |
| KAFKA_LISTENERS | PLAINTEXT://kafka:9092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:29092 | KAFKA가 수신할 주소와 포트 설정 |
| KAFKA_ADVERTISED_LISTENERS | PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092 | 클라이언트에게 공개할 리스너 주소   |
| KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR | 1 | 오프셋 토픽 복제 팩터         |
| KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR | 1 | 오프셋 토픽 복제 팩터         |
| KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR | 1 | 트랜잭션 상태 로그 복제 팩터     |

```yml
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "29092:29092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://kafka:9092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:29092'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      CLUSTER_ID: 'ciWo7IWazngRchmPES6q5A=='
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
    kafka-ui:
      image: provectuslabs/kafka-ui:latest
      ports:
        - "8080:8080"
      environment:
        KAFKA_CLUSTERS_0_NAME: local
        KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      depends_on:
        - kafka
```

### Kafka topic 생성 및 조회
kafka 컨테이너 접근 및 kafka-topic 명령어로 topic 생성
조회는 kakfa-topic --list로 조회

```
host> docker exec -it kafka-study-kafka-1 bash
[appuser@517c2872818d ~]$ kafka-topics --create --topic hello-world --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
Created topic hello-world.

[appuser@517c2872818d ~]$ kafka-topics --list --bootstrap-server kafka:9092
__consumer_offsets
hello-world
```

### Kafka producer, consumer 테스트

#### Kafka producer 메시지 발행
kafka-console-producer를 통해서 

```
host> docker exec -it kafka-study-kafka-1 bash
[appuser@517c2872818d ~]$ 
[appuser@517c2872818d ~]$ kafka-console-producer --topic hello-world --bootstrap-server kafka:9092
>hello
```

#### Kafka consumer 메시지 구독(소비)

```
host> docker exec -it kafka-study-kafka-1 bash
[appuser@517c2872818d ~]$
[appuser@517c2872818d ~]$ kafka-console-consumer --topic hello-world --bootstrap-server kafka:9092 --from-beginning
hello
```