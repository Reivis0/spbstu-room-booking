package com.github.MadyarovGleb.booking_mvp.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthRequests {
    private String email;
    private String password;
}

