package com.fiscalimplify.fiscalimplify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfeImportXmlResponse {
    private Map<String, Object> emissionResult;
    private NfeRequest parsedRequest;
}

