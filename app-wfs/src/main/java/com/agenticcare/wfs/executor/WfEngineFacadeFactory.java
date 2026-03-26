package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WfEngineFacadeFactory {

    private static final Logger log = LoggerFactory.getLogger(WfEngineFacadeFactory.class);
    private final Map<String, WfEngineFacade> facadeMap;

    public WfEngineFacadeFactory(List<WfEngineFacade> facades) {
        this.facadeMap = facades.stream()
                .collect(Collectors.toMap(WfEngineFacade::getEngineType, Function.identity()));
        log.info("Registered WfEngineFacades: {}", facadeMap.keySet());
    }

    public WfEngineFacade getFacade(String engineType) {
        WfEngineFacade facade = facadeMap.get(engineType);
        if (facade == null) {
            throw new UnsupportedOperationException("No facade for engine type: " + engineType);
        }
        return facade;
    }
}
