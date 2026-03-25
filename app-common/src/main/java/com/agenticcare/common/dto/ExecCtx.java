package com.agenticcare.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecCtx<REQ, RESP> {
    private REQ reqDto;
    private RESP respDto;
}
