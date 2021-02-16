package winterschoolone.external;

public class Payment {

    private Long id;
    private Long orderId;
    private String userId;
    private String menuId;
    private Integer qty;
    private String cancelYn;

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
