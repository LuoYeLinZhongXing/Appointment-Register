package com.luoye.constant;

/**
 * 信息提示常量类
 * 按功能模块分类整理的消息常量
 */
public class MessageConstant {

    //通用错误消息

    public static final String UNKNOWN_ERROR = "未知错误";

    public static final String PARAMETER_EMPTY = "必要参数为空";

    //参数类型错误相关
    public static final String PARAMETER_TYPE_MISMATCH = "参数格式不正确，请输入有效的数字";

    public static final String ID_FORMAT_ERROR = "ID格式不正确，请输入有效的数字";

    public static final String NUMBER_FORMAT_ERROR = "数字格式不正确";

    public static final String STATUS_INVALID = "状态值错误";

    //用户认证相关

    public static final String PHONE_ERROR = "手机号已注册";

    public static final String PHONE_PASSWORD_NOT_NULL = "手机号或密码不能为空";

    public static final String NAME_NOT_NULL = "姓名不能为空";

    public static final String LOGIN_ERROR = "用户名或密码错误";

    public static final String USER_NOT_LOGIN = "用户未登录或信息缺失";

    public static final String NO_PERMISSION = "没有权限执行此操作";

    public static final String NAME_LENGTH_ERROR = "名称长度需要2到10个字符";

    public static final String PHONE_NOT_FOUND = "手机号不存在";

    public static final String NEW_PASSWORD_EMPTY = "新密码不能为空";

    public static final String PASSWORD_RESET_SUCCESS = "密码重置成功";

    //患者管理相关

    public static final String EMAIL_FORMAT_ERROR = "邮箱格式错误";

    public static final String GENDER_ERROR = "性别格式错误";
    //医生管理相关

    public static final String DOCTOR_NOT_FOUND = "医生不存在";

    public static final String DOCTOR_NAME_EMPTY = "医生姓名不能为空";

    public static final String DOCTOR_NAME_TOO_LONG = "医生姓名长度不能超过50个字符";

    public static final String PHONE_FORMAT_ERROR = "手机号格式不正确";

    public static final String PASSWORD_EMPTY = "密码不能为空";

    public static final String DEPT_ID_EMPTY = "科室ID不能为空";

    public static final String GENDER_INVALID = "性别必须为0(男)或1(女)";

    public static final String POST_INVALID = "医生职位必须为0(普通医生)或1(科室主任)";

    public static final String CARD_FORMAT_ERROR = "身份证号格式不正确";

    public static final String DEPT_HEAD_EXISTS = "该科室已存在主任，无法重复设置";

    public static final String PATIENT_NOT_FOUND = "未找到患者";

    public static final String ADMIN_NOT_EXIST = "管理员不存在";

    //科室管理相关

    public static final String CREATE_SUCCESS = "创建成功";

    public static final String DEPT_NOT_FOUND = "未找到科室";

    public static final String DEPT_NAME_EMPTY = "科室名称不能为空";

    public static final String DEPT_NAME_TOO_LONG = "科室名称长度不能超过50个字符";

    public static final String DEPT_TYPE_EMPTY = "科室类型不能为空";

    public static final String DEPT_TYPE_INVALID = "科室类型必须为0(临床)或1(医技)";

    public static final String PAGE_NUMBER_INVALID = "页码必须大于等于1";

    public static final String PAGE_SIZE_INVALID = "页面大小必须大于等于1";

    public static final String PAGE_SIZE_TOO_LARGE = "页面大小不能超过100";

    public static final String SORT_FIELD_INVALID = "排序字段不存在";

    public static final String SORT_DIRECTION_INVALID = "排序方向必须为asc或desc";

    public static final String DEPT_LOCATION_TOO_LONG = "科室位置长度过长";

    public static final String DEPT_DESCRIPTION_TOO_LONG = "科室描述过长";

    //号源管理相关

    public static final String SLOT_ALREADY_EXISTS = "该医生在指定日期和时段已存在号源，不能重复放号";

    public static final String SLOT_NOT_AVAILABLE = "号源不存在或已约满";

    public static final String DOCTOR_ONLY_SELF_SLOT = "医生只能放出自己的号源";

    public static final String DIRECTOR_ONLY_RELEASE_SLOT = "只有科室主任才能放出号源";

    //订单管理相关

    public static final String ORDER_CREATE_FAILED = "创建订单失败";

    public static final String ORDER_NOT_FOUND = "订单不存在";

    public static final String DUPLICATE_REGISTRATION_ERROR = "您已在相同时间段预约了其他号源，请勿重复挂号";

    public static final String ORDER_HAS_BEEN_PAID = "订单已支付";

    public static final String ORDER_CANCEL = "订单已取消";

    public static final String ORDER_CHECK_IN = "已就诊";

    public static final String ORDER_STATUS_ERROR = "订单状态异常";

    public static final String PAYMENT_FAILED = "支付失败";

    public static final String ORDER_CANCEL_SUCCESS = "订单取消成功";

    public static final String ORDER_CHECK_IN_SUCCESS = "报到成功";

    public static final String CANCEL_REASON_REQUIRED = "取消原因不能为空";

    public static final String ORDER_ALREADY_CHECKED_IN = "订单已取号";


    //通用操作成功消息

    public static final String ALREADY_EXISTS = "已存在";

    public static final String UPDATE_SUCCESS = "更新成功";

    public static final String DELETE_SUCCESS = "删除成功";

    public static final String STATUS_UPDATE_SUCCESS = "状态更新成功";

    public static final String REGISTER_SUCCESS = "注册成功";


    public static final String EMERGENCY_STATUS_INVALID = "急诊标识为空";
    public static final String SLOT_COUNT_INVALID = "最大号源数缺失";
    public static final String SLOT_COUNT_TOO_LARGE = "最大号源数过大";
    public static final String SLOT_DATE_INVALID = "号源日期错误";
    public static final String FEE_AMOUNT_INVALID = "挂号费存在异常";
    public static final String DOCTOR_NOT_AVAILABLE = "医生已离职";
    public static final String DOCTOR_DEPT_NOT_ASSIGNED = "科室不存在";
    public static final String DEPT_NOT_AVAILABLE = "科室已停用";
    public static final String SLOT_NOT_EXIST = "号源不存在";
    public static final String SLOT_HAS_BOOKING = "号源已有人预约，无法取消";
    public static final String NO_PATIENT_TO_CALL = "当前没有等待的患者";

    public static final String CALL_SUCCESS = "叫号成功";

    public static final String RECALL_SUCCESS = "重新叫号成功";

    public static final String QUEUE_IS_EMPTY = "队列为空";

    public static final String SYSTEM_BUSY = "系统繁忙，请稍后重试";
    public static final String QUEUE_NOT_FOUND = "未找到排队记录";
    public static final String QUEUE_STATUS_ERROR = "排队记录状态错误";
    public static final String UPDATE_FAILED = "更新失败";
    public static final String ADMIN_REGISTER_FAILED = "管理员注册失败";
    public static final String REGISTER_ERROR = "注册失败";
    public static final String DELETE_FAILED = "删除失败";
    public static final String ORDER_REGISTER_SUCCESS = "挂号成功，已生成订单";
    public static final String ORDER_PAY_SUCCESS = "支付成功";
    public static final String ORDER_PAY_FAILURE = "支付失败，请稍后重试";
}
