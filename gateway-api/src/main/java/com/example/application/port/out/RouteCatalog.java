package com.example.application.port.out;

import com.example.domain.model.DownstreamRoute;

import java.util.List;
import java.util.Optional;

public interface RouteCatalog {
    Optional<DownstreamRoute> findByPublicSegment(String publicSegment);

    List<DownstreamRoute> list();
}
