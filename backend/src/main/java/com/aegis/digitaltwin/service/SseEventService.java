package com.aegis.digitaltwin.service;

import com.aegis.digitaltwin.dto.FraudDecisionResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEventService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        return emitter;
    }

    public void publishDecision(FraudDecisionResponse response) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("fraud-decision").data(response));
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }
}
