package winterschoolone;

import winterschoolone.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class SirenOrderHomeViewHandler {


    @Autowired
    private SirenOrderHomeRepository sirenOrderHomeRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenPayed_then_CREATE_1 (@Payload Payed payed) {
        try {
            if (payed.isMe()) {
            	// view 객체 생성
            	SirenOrderHome sirenOrderHome = new SirenOrderHome();
                // view 객체에 이벤트의 Value 를 set 함
            	sirenOrderHome.setOrderId(payed.getOrderId());
            	sirenOrderHome.setUserId(payed.getUserId());
            	sirenOrderHome.setMenuId(payed.getMenuId());
            	sirenOrderHome.setPayId(payed.getId());
            	sirenOrderHome.setQty(payed.getQty());
            	sirenOrderHome.setStatus("Payed");
                // view 레파지 토리에 save
            	sirenOrderHomeRepository.save(sirenOrderHome);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenAssigned_then_UPDATE_1(@Payload Assigned assigned) {
        try {
            if (assigned.isMe()) {
                // view 객체 조회
            	List<SirenOrderHome> sirenOrderHomeList = sirenOrderHomeRepository.findByOrderId(assigned.getOrderId());
                for(SirenOrderHome sirenOrderHome : sirenOrderHomeList){
                	sirenOrderHome.setShopId(assigned.getId());
                	sirenOrderHome.setStatus("Assigned");
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
                	sirenOrderHomeRepository.save(sirenOrderHome);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrderCancelled_then_UPDATE_2(@Payload OrderCancelled orderCancelled) {
        try {
            if (orderCancelled.isMe()) {
                // view 객체 조회
            	List<SirenOrderHome> sirenOrderHomeList = sirenOrderHomeRepository.findByOrderId(orderCancelled.getId());
            	for(SirenOrderHome sirenOrderHome : sirenOrderHomeList){
                	sirenOrderHome.setShopId(orderCancelled.getId());
                	sirenOrderHome.setStatus("Refunded");
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
                	sirenOrderHomeRepository.save(sirenOrderHome);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenRefunded_then_DELETE_1(@Payload Refunded refunded) {
        try {
            if (refunded.isMe()) {
            	List<SirenOrderHome> sirenOrderHomeList = sirenOrderHomeRepository.findByOrderId(refunded.getOrderId());
                for(SirenOrderHome sirenOrderHome : sirenOrderHomeList){
                	sirenOrderHome.setShopId(refunded.getId());
                	sirenOrderHome.setStatus("Refunded");
                	sirenOrderHome.setPoint(0);
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
                	sirenOrderHomeRepository.save(sirenOrderHome);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPointSaved_then_UPDATE_3(@Payload PointSaved pointSaved) {
        try {
            if (pointSaved.isMe()) {
            	List<SirenOrderHome> sirenOrderHomeList = sirenOrderHomeRepository.findByOrderId(pointSaved.getOrderId());
                for(SirenOrderHome sirenOrderHome : sirenOrderHomeList){
                	sirenOrderHome.setPoint(pointSaved.getPoint());
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    // view 레파지 토리에 save
                	sirenOrderHomeRepository.save(sirenOrderHome);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}