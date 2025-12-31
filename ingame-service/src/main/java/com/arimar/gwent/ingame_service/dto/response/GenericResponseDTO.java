package com.arimar.gwent.ingame_service.dto.response;


public class GenericResponseDTO<T> {

    private String serviceOrigin;
    private Integer status;
    private T data;

    public GenericResponseDTO(String serviceProcedencia, Integer status, T data) {
        this.serviceOrigin = serviceProcedencia;
        this.status = status;
        this.data = data;
    }

    public String getServiceOrigin() {
        return serviceOrigin;
    }

    public void setServiceOrigin(String serviceOrigin) {
        this.serviceOrigin = serviceOrigin;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
