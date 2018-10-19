package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * A schema.
 */
public class SchemaV1 extends MappedSchema {
    public SchemaV1() {
        super(Schema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "finance") private final String finance;
        @Column(name = "bank") private final String bank;
        @Column(name = "creditA") private final String creditA;
        @Column(name = "company") private final String company;
        @Column(name = "value") private final int value;
        @Column(name = "linear_id") private final UUID linearId;


        public PersistentIOU(String finance, String bank,String creditA,String company, int value, UUID linearId) {
            this.finance = finance;
            this.bank = bank;
            this.creditA = creditA;
            this.company = company;
            this.value = value;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.finance = null;
            this.bank = null;
            this.creditA = null;
            this.company = null;
            this.value = 0;
            this.linearId = null;
        }

        public int getValue()
        {
            return value;
        }

        public UUID getId()
        {
            return linearId;
        }

        public String getCreditA()
        {
            return creditA;
        }

        public String getCompany() {
            return company;
        }

        public String getFinance() {
            return finance;
        }

        public String getBank() {
            return bank;
        }
    }
}