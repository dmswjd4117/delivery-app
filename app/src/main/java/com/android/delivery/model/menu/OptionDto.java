package com.android.delivery.model.menu;

public class OptionDto {
    public String name;
    public Long menuId;
    public Long price;

    public String getName() {
        return name;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }
}
