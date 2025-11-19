package com.safebox.home.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class SecretEntry {
    private String id;
    private String title;
    private String username;
    private String password;
    private String notes;
    private String category;



 
}
