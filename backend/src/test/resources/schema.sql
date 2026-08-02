create table conversation (
    id uuid not null,
    title varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (id)
);

create table message (
    id uuid not null,
    conversation_id uuid not null,
    role varchar(255),
    content varchar(4000),
    tool_name varchar(255),
    created_at timestamp with time zone not null,
    primary key (id),
    constraint fk_message_conversation foreign key (conversation_id) references conversation (id)
);

