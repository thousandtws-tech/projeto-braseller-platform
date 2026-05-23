package com.example.application.port.out;

import com.example.application.dto.DownstreamRequest;
import com.example.application.dto.GatewayResponse;

public interface DownstreamServiceClient {
    GatewayResponse exchange(DownstreamRequest request);
}
