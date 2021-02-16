package winterschoolone;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import winterschoolone.external.Point;
import winterschoolone.external.PointService;

import java.util.List;

@Entity
@Table(name="Payment_table")
public class Payment {
	
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String userId;
    private String menuId;
    private Integer qty;
    private String cancelYn;

    @PostPersist
    public void onPostPersist(){
        Payed payed = new Payed();
        BeanUtils.copyProperties(this, payed);
        payed.publishAfterCommit();
        
        //try {
        //        Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        //} catch (InterruptedException e) {
        //        e.printStackTrace();
        //}
    }

    @PostUpdate
    public void PostUpdate(){
        Refunded refunded = new Refunded();
        BeanUtils.copyProperties(this, refunded);
        refunded.publishAfterCommit();
        
        System.out.println("### Point 취소_PostUpdate");
        Point point = new Point();
        point.setId(this.getId());
        point.setOrderId(this.getOrderId());
        point.setUserId(this.getUserId());
        point.setPoint(0);
        PaymentApplication.applicationContext.getBean(PointService.class)
        .point(point);

    }
    
    @PreRemove
    public void PreRemove(){
        Refunded refunded = new Refunded();
        BeanUtils.copyProperties(this, refunded);
        refunded.publishAfterCommit();
        
        System.out.println("### Point 취소_PreRemove");
        Point point = new Point();
        point.setId(this.getId());
        point.setOrderId(this.getOrderId());
        point.setUserId(this.getUserId());
        point.setPoint(0);
        PaymentApplication.applicationContext.getBean(PointService.class)
        .point(point);
        
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
    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getCancelYn() {
        return cancelYn;
    }

    public void setCancelYn(String cancelYn) {
        this.cancelYn = cancelYn;
    }




}
