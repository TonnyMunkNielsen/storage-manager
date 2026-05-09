-- Create produce_type table
CREATE TABLE produce_type
(
    id                         UUID PRIMARY KEY,
    name                       VARCHAR(255) NOT NULL UNIQUE,
    description                TEXT,
    price                      NUMERIC(8, 2),
    image_data                 BYTEA,
    image_content_type         VARCHAR(100),
    image_filename             VARCHAR(255),
    notification_days_modifier INTEGER      NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE storage_box
(
    id                     UUID PRIMARY KEY,
    dessicant_changed_date DATE        NOT NULL,
    box_number             INTEGER     NOT NULL,
    status                 VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create produce_instance table
CREATE TABLE produce_instance
(
    id               UUID PRIMARY KEY,
    produce_type_id  UUID         NOT NULL,
    storage_box_id   UUID         NOT NULL,
    replaced_by_id   UUID,
    title            VARCHAR(255) NOT NULL,
    best_before_date DATE         NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    replaced_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (produce_type_id) REFERENCES produce_type (id) ON DELETE CASCADE,
    FOREIGN KEY (storage_box_id) REFERENCES storage_box (id),
    FOREIGN KEY (replaced_by_id) REFERENCES produce_instance (id)
);

-- Create notification table
CREATE TABLE notification
(
    id                UUID PRIMARY KEY,
    target_id         UUID        NOT NULL,
    target_type       VARCHAR(50) NOT NULL, -- 'PRODUCE_INSTANCE' or 'STORAGE_BOX'
    notification_type VARCHAR(50) NOT NULL,
    message           TEXT        NOT NULL,
    sent_at           TIMESTAMP,
    status            VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create app_user table
CREATE TABLE app_user
(
    id            UUID PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Create indexes for better performance
CREATE INDEX idx_produce_instance_produce_type_id ON produce_instance (produce_type_id);
CREATE INDEX idx_produce_instance_storage_box_id ON produce_instance (storage_box_id);
CREATE INDEX idx_produce_instance_best_before_date ON produce_instance (best_before_date);
CREATE INDEX idx_produce_instance_status ON produce_instance (status);
CREATE INDEX idx_notification_target ON notification (target_id, target_type);
CREATE INDEX idx_notification_status ON notification (status);
CREATE INDEX idx_app_user_username ON app_user (username);

-- Create trigger to update updated_at timestamp
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
language 'plpgsql';

CREATE TRIGGER update_produce_type_updated_at
    BEFORE UPDATE
    ON produce_type
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_produce_instance_updated_at
    BEFORE UPDATE
    ON produce_instance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notification_updated_at
    BEFORE UPDATE
    ON notification
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_storage_box_updated_at
    BEFORE UPDATE
    ON storage_box
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_app_user_updated_at
    BEFORE UPDATE
    ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();