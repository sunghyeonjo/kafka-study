# kafka-study

최근 Confluent에서 업로드한 카프카의 파티션 리더 선출과 관련한 1시간 남짓한 영상을 보게 되었습니다.

꽤 유익한 내용이었던 것 같아, 이번 글에서는 영상의 내용을 요약 정리해보고 다른 레퍼런스를 참고해 공부해 본 내용을 정리해보려고 합니다.

(아래에서 설명하는 내용 중 리더 파티션과 브로커를 혼용해서 사용했는데, 리더 브로커가 좀 더 적합한 표현이지만 개별 파티션 관점에서 설명하기 위해 리더 파티션이란 용어를 썼습니다.)


Kafka Partition Leader?
Partition Leader Election(리더 선출)은 카프카의 확장성(Scalability)와 성능(Performance)에 중요한 역할을 합니다.

이에 대해 설명하기 위해서는 먼저 Kafka Partition Leader가 어떤 역할을 하며 리더라는 개념이 왜 필요한지에 대해 짚어봐야겠습니다.

일반적으로 카프카 클러스터를 구성할 때 여러 브로커에 다수의 파티션을 갖는 토픽을 생성하는데요. 이 때, N개의 파티션 중에 리더 파티션은 단 하나만 존재하며 나머지 파티션은 팔로워(Follower) 파티션이 되어 사용자가 replication-factor로 지정한 수 만큼의 Replica를 구성하게 됩니다.

그리고 Replica를 구성하는 여러 방식 중에 카프카는 효율적이고 간단한 시스템 구축을 위해 '클라이언트와 리더 파티션 간의 1:1 커뮤니케이션'에 집중하는데요.

카프카 클라이언트는 metadata 요청을 사용하여 토픽의 파티션 정보와 각 파티션의 Replica 정보(Leader, Follwer) 등을 응답받습니다. 그리고 이 데이터를 캐시해두었다가 각 파티션의 리더 브로커에게 읽기, 쓰기 작업을 요청합니다. 

metadata 정보는 모든 브로커 간에 공유되기 때문에 살아있는 임의의 브로커에 metadata 요청을 보내고, 응답받은 파티션 정보를 바탕으로 특정 브로커에 요청을 보내는 것이죠. 다중 브로커가 있는 상황에서도 --bootstrap-server 옵션값에 임의의 브로커 하나만 명시해도 클라이언트 요청을 할 수 있는 것이 브로커 간 metadata가 공유된다는 사실에서 설명 가능합니다. 물론 metadata는 metadata.max.age.ms 매개변수로 새로고침 및 동기화됩니다.

한편, 클라이언트는 ack 값을 통해 Replica가 의도한만큼 잘 복사되었다는 응답을 받아 데이터의 유실이 없음을 확인하고 팔로워 파티션들은 리더 파티션에 데이터 PULL 요청을 보냄으로써 데이터를 복제합니다. 

이 때, 리더 파티션이 팔로워 파티션에 데이터를 PUSH 하는 것이 아닌 팔로워가 리더에게 PULL 요청을 한다는 것이 흥미로운 부분인데요.
디자인 패턴 중 옵저버 패턴(Observer Pattern)에서도 주체(Subject)는 옵저버(Observer)에게 데이터의 변화를 PUSH 하는 대신 옵저버가 주체로부터 데이터를 가져오는 방식을 선호합니다.

리더 파티션은 다수의 팔로워 파티션들을 관리할 필요성이 줄어들게 되고 개별 팔로워 파티션들의 데이터 복사 결과에 독립적으로 작업을 수행할 수 있어 클라이언트와 작업에서의 성능에 집중하게 되는 것입니다.

![IMG_0575](https://github.com/sunghyeonjo/kafka-study/assets/61929745/c4c60a1f-9100-49c4-a17e-9486b8f9b9c8)

또한, 대부분의 Consumer들은 Producer가 발행한 최근 데이터를 거의 즉각적으로 소비합니다. 이 때, 리더 파티션은 Producer가 쓴 데이터를 최초에는 메모리에 저장하고 디스크에 쓰기 때문에 Consumer 입장에서도 READ 요청에 대한 응답이 빨라질 수 있게 됩니다.


Partition Leader Election 동작 원리
Partition Leader Election은 '리더 파티션를 담당했던 브로커에 장애가 생겼을 때 해당 리더를 어떻게 대체할 것인가?'에 대한 내용입니다.

이에 대해 설명하기 위해 간단한 개념 세 가지를 먼저 짚고 넘어가겠습니다.

[1] 위에서 ISR(In-Sync Replica)에 대해 잠깐 언급했는데요. 리더 파티션은 팔로워 파티션의 PULL 요청의 마지막 Offset 값을 활용하여 각 팔로워 파티션의 LAG을 체크합니다. 이 때, 팔로워 파티션의 PULL 요청이 꾸준히 들어오면서 Offset이 최근의 데이터를 반영할 때만 ISR로 취급하게 됩니다.

[2] 우선 최초에 어떤 파티션이 Leader, Follower가 될지는 어떻게 정할까요? 이에 대해 알기 위해서는 Preferred Reader(선호 리더)라는 개념을 알아야 합니다.

Preferred Leader란 토픽이 처음 생성될 때 리더였던 Replica를 말합니다. 일반적으로 파티션 분배는 여러 브로커에 대해 Round-Robin 방식으로 균등하게 진행되기 때문에, 모든 파티션에 대해 Preferred Leader가 실제 리더가 될 경우 브로커 사이의 리더 역할이 균등하게 분배되어 부하 분산이 원활히 될 것을 기대할 수 있게 됩니다.
예를 들어, 3개의 파티션과 replication factor = 3인 토픽 A를 생성했을 때 1,2,3번 순서대로 Replica가 구성된다면 Preferred Leader는 1번이 됩니다.

물론 RR 방식이 완전한 부하 균등을 보장할 수는 없습니다. 파티션, 리더 브로커의 성능, 토픽의 Workload에 따라서 부하는 달라지기 때문에 이 역시 다이나믹하게 바뀔 수는 있어 Cruise Control 같은 도움을 빌릴 수도 있습니다.

다만, 설명을 위한 일반적인 상황에서 Preferred Leader를 알기 위해서는 --describe 명령어를 통해 토픽 파티션의 Replica 리스트를 확인하면 됩니다. Replica 목록의 첫번째 녀석이 Preferred Leader입니다.

```
./kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic test-topic
Topic: test-topic	TopicId: 9Roa7lhAT92q5GQk_ha1vA	PartitionCount: 3	ReplicationFactor: 3	Configs: segment.bytes=1073741824
	Topic: test-topic	Partition: 0	Leader: 2	Replicas: 2,3,1	Isr: 2,3,1
	Topic: test-topic	Partition: 1	Leader: 3	Replicas: 3,1,2	Isr: 3,1,2
	Topic: test-topic	Partition: 2	Leader: 1	Replicas: 1,2,3	Isr: 1,2,3
```
위 결과에서 Partition 0의 Preferred Leader는 2번입니다.

[3] 컨트롤러는 일반적인 카프카 브로커의 기능에 더해서 파티션 리더를 선출하는 역할을 추가적으로 맡습니다.

클러스터에서 가장 먼저 시작되는 브로커는 Zookeeper의 /controller에 Ephemeral 노드를 생성함으로써 컨트롤러가 되는데요.

브로커들은 주키퍼의 컨트롤러 노드에 변동이 생겼을 때 알림을 받기 위해 해당 노드에 Watch를 설정하고, 컨트롤러 브로커가 멈추거나 주키퍼와의 연결이 끊겼을 경우 Ephemeral 노드가 삭제되게 됩니다.

그리고 클러스터 안의 다른 브로커들은 주키퍼에 설정된 Watch를 통해 컨트롤러의 부재를 알게 되고 주키퍼에 컨트롤러 노드를 생성하려고 재시도를 하게 됩니다.

그러면 이제 Partition Leader Election에 대해 설명하기 위해 몇 가지 시나리오를 생각해보겠습니다.

[1] 만약 리더였던 1번이 죽고 2,3번 Replica가 ISR에 포함된 상태라면 어떻게 될까요?
이 때는 리더였던 1번 Replica의 바로 다음 Replica인 2번이 리더로 선출되게 됩니다. 

하지만 이 때는 위에서 말씀드렸던 것처럼 리더 파티션이 적절히 밸런싱된 상태가 아니기 때문에 2번 Replica가 기존보다 더 많은 역할을 수행하고 있게 됩니다.
따라서 1번 브로커가 정상으로 돌아오고 적절한 시간이 지난 뒤 1번 Replica가 ISR 상태라면 Preferred Leader Election이 진행되어 다시 1번 브로커가 리더로 선출되게 됩니다.

물론, 위에서도 말씀드렸듯이 카프카 클러스터의 개별적인 상황에 따라 특정 파티션을 리더로 위임해야 한다면 개발자가 직접 Partition Reassign 명령을 통해 리더 선출을 제어할 수도 있습니다.

[2] 만약 리더 브로커가 죽고 다른 모든 Replica도 ISR 상태가 아니라면 어떻게 될까요?
이 때는 사실 해당 파티션의 리더가 없게 되고 시간이 지날수록 데이터의 유실이 발생하게 됩니다. 리더 파티션이 정상 상태로 돌아올 때까지 한없이 기다리기만 해야하죠.

따라서, Offset 차이만큼의 데이터 유실을 감수하고서라도 Out of Sync 상태의 Replica를 강제로 리더로 선출하게 만들어 버릴 수 있는데 이를 Unclean Leader Election이라고 합니다.

물론 데이터의 정합성이 정말 중요한 상황이라면 이 선택을 하는 데 신중해야 합니다..

한편 Unclean Leader Election은 토픽 단위로 unclean.leader.election.enable=true 옵션 설정 뒤 진행할 수 있습니다.
```
kafka-configs.sh --bootstrap-server localhost:9092 --alter --entity-type topics --entity-name configured-topic --add-config unclean.leader.election.enable=true
```




