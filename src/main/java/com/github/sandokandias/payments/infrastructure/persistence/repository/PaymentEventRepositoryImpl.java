package com.github.sandokandias.payments.infrastructure.persistence.repository;


import com.github.sandokandias.payments.domain.entity.PaymentEventRepository;
import com.github.sandokandias.payments.domain.event.PaymentRequested;
import com.github.sandokandias.payments.domain.vo.PaymentEventId;
import com.github.sandokandias.payments.domain.vo.PaymentEventType;
import com.github.sandokandias.payments.infrastructure.persistence.table.PaymentEventTable;
import com.github.sandokandias.payments.infrastructure.util.serialization.JsonMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Repository
class PaymentEventRepositoryImpl implements PaymentEventRepository {

    private final EventStore eventStore;
    private final JsonMapper jsonMapper;
    private final Executor pool;

    PaymentEventRepositoryImpl(EventStore eventStore,
                               JsonMapper jsonMapper) {
        this.eventStore = eventStore;
        this.jsonMapper = jsonMapper;
        this.pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public CompletionStage<PaymentEventId> store(PaymentRequested paymentRequested) {
        return CompletableFuture.supplyAsync(() -> {
            String eventDataAsJson = jsonMapper.write(paymentRequested);
            PaymentEventTable paymentEventTable = new PaymentEventTable();
            paymentEventTable.setId(paymentRequested.paymentEventId.id);
            paymentEventTable.setEventType(PaymentEventType.PAYMENT_REQUESTED);
            paymentEventTable.setPaymentId(paymentRequested.paymentId.id);
            paymentEventTable.setCreatedAt(LocalDateTime.now());
            paymentEventTable.setEventData(eventDataAsJson);
            eventStore.save(paymentEventTable);
            return paymentRequested.paymentEventId;
        }, pool);
    }
}