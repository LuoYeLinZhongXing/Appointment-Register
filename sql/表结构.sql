-- auto-generated definition
create table admin
(
    id          bigint auto_increment comment '管理员id'
        primary key,
    name        varchar(256)                       null comment '管理员名称',
    password    varchar(256)                       null comment '管理员密码',
    gender      tinyint                            null comment '性别：0男 1女',
    card        varchar(256)                       null comment '身份证号',
    phone       varchar(128)                       not null comment '手机号',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '管理员表';


-- auto-generated definition
create table doctor
(
    id           bigint auto_increment comment '医生id'
        primary key,
    name         varchar(50)                        not null comment '医生姓名',
    phone        varchar(20)                        not null comment '手机号',
    password     varchar(255)                       not null comment '密码',
    gender       tinyint  default 0                 null comment '性别: 0.男 1.女',
    card         char(18)                           null comment '身份证号',
    status       tinyint  default 1                 null comment '状态: 1.在职 2.离职',
    post         tinyint                        null comment '医生职位',
    introduction text                               null comment '医生简介',
    dept_id      bigint                             not null comment '隶属科室',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint doctor_pk_2
        unique (card)
)
    comment '医生表';

-- auto-generated definition
create table dept
(
    id          bigint auto_increment comment '科室id'
        primary key,
    name        varchar(50)                        null comment '科室名称',
    type        tinyint  default 0                 null comment '科室类型 :0.临床 1.医技',
    location    varchar(100)                       null comment '科室位置',
    status      tinyint  default 1                 null comment '科室状态: 0.停用 1.启用',
    description text                            null comment '简介',
    director_id bigint                             null comment '负责人id',
    create_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '科室表';


-- auto-generated definition
create table log
(
    id               bigint auto_increment comment '日志id'
        primary key,
    log_type         varchar(20)                        not null comment '操作类型: 增删改查',
    operator_type    varchar(10)                        not null comment '操作人类型: 管理员/医生/患者/系统',
    operator_id      bigint                             null comment '操作人id',
    operator_name    varchar(50)                        null comment '操作人姓名',
    target_type      varchar(20)                        null comment '目标类型 : 订单/号源 等',
    target_id        bigint                             null comment '目标id',
    operation_detail text                               null comment '操作详情',
    ip_address       varchar(45)                        null comment 'ip地址',
    success_flag     tinyint  default 1                 null comment '操作结果 : 0.失败 1.成功',
    error_message    text                               null comment '失败消息',
    create_time      datetime default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '日志表';

-- auto-generated definition
create table `order`
(
    id            bigint auto_increment comment '订单id'
        primary key,
    order_no      varchar(20)                        not null comment '订单号',
    patient_id    bigint                             not null comment '患者id',
    slot_id       bigint                             not null comment '号源id',
    doctor_id     bigint                             not null comment '医生id',
    dept_id       bigint                             not null comment '科室id',
    fee_amount    decimal(10, 2)                     not null comment '挂号费',
    order_status  tinyint  default 1                 null comment '订单状态：1.待支付 2.已支付 3.已取消 4.已就诊',
    payment_time  datetime                           null comment '支付时间',
    check_in_time datetime                           null comment '取号/报到时间',
    cancel_time   datetime                           null comment '取消时间',
    is_emergency  tinyint  default 0                 null comment '是否急诊：0普通，1急诊',
    cancel_reason text                               null comment '取消原因',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '订单表';



-- auto-generated definition
create table patient
(
    id          bigint auto_increment comment '患者id'
        primary key,
    name        varchar(50)                        not null comment '患者姓名',
    password    varchar(100)                       not null comment '患者密码',
    gender      tinyint                            null comment '性别 :0.男 1.女',
    card        char(18)                           null comment '身份证号',
    phone       varchar(20)                        not null comment '手机号',
    email       varchar(255)                       null comment '邮箱',
    address     varchar(255)                       null comment '地址',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '患者表';

-- auto-generated definition
create table slot
(
    id            bigint auto_increment comment '号源id'
        primary key,
    dept_id       bigint                             not null comment '科室id',
    doctor_id     bigint                             not null comment '医生id',
    schedule_date date                               not null comment '出诊日期',
    time_period   varchar(10)                        not null comment '时间段: morning/afternoon/night',
    total_count   int      default 200               not null comment '总号源数',
    booked_count  int      default 0                 not null comment '已预约数',
    fee_amount    decimal(10,2)                      not null comment '挂号费用',
    status        tinyint  default 1                 null comment '号源状态：1.可预约, 0.已停诊, 2.已约满',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '号源表';


-- auto-generated definition
create table queue
(
    id                 bigint auto_increment comment '排队id'
        primary key,
    order_id           bigint                             not null comment ' 订单id',
    patient_id         bigint                             not null comment '患者id',
    doctor_id          bigint                             not null comment '医生id',
    dept_id            bigint                             not null comment '科室id',
    queue_number       int                                not null comment '排队号码(每个医生独立)',
    queue_status       tinyint  default 1                 null comment '排队状态：1等待中, 2呼叫中, 3就诊中, 4已完成, 5过号, 6已取消',
    is_priority        tinyint  default 0                 null comment '是否优先：0普通，1优先',
    check_in_time      datetime                           null comment '报到时间',
    call_time          datetime                           null comment '呼叫时间',
    start_time         datetime                           null comment '开始就诊时间',
    end_time           datetime                           null comment '结束就诊时间',
    missed_count       tinyint  default 0                 null comment '过号次数',
    max_missed_allowed tinyint  default 3                 null comment '最大允许过号次数（默认3次）',
    create_time        datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '排队表';


