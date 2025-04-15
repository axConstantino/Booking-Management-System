package com.axconstantino.reservationsystem.constants;

public class ValidationMessages {
    // Name validations
    public static final String REGISTER_NAME_REQUIRED = "Full name is required";
    public static final String REGISTER_NAME_SIZE = "Name must be between {min} and {max} characters";

    // Email validations
    public static final String REGISTER_EMAIL_REQUIRED = "Email address is required";
    public static final String REGISTER_EMAIL_INVALID = "Please provide a valid email address";

    // Password validations
    public static final String REGISTER_PASSWORD_REQUIRED = "Password is required";
    public static final String REGISTER_PASSWORD_PATTERN =
            "Password must contain: 8+ characters, 1 uppercase, 1 number, and 1 special character (@#$%^&+=)";

    // User validations
    public static final String USER_NAME_BLANK = "User name cannot be blank";
    public static final String USER_NAME_LENGTH = "User name must be between {min} and {max} characters";
    public static final String USER_PASSWORD_BLANK = "Password cannot be blank";
    public static final String USER_EMAIL_BLANK = "Email cannot be blank";
    public static final String USER_EMAIL_INVALID = "Email must be a valid format";
    public static final String USER_PHONE_INVALID = "Phone number must be in valid international format";
    public static final String USER_ROLES_EMPTY = "User must have at least one role";

    //Room validations
    public static final String ROOM_NAME_REQUIRED = "Room name is required";
    public static final String ROOM_NAME_LENGTH = "Room name must be between {min} and {max} characters";
    public static final String ROOM_PRICE_POSITIVE = "Price per night must be greater than 0";
    public static final String ROOM_CAPACITY_MIN = "Minimum room capacity is {min} person";
    public static final String ROOM_CAPACITY_MAX = "Maximum room capacity is {max} people";
    public static final String ROOM_TYPE_REQUIRED = "Room type is required";
    public static final String ROOM_DESC_LENGTH = "Description cannot exceed {max} characters";
    public static final String ROOM_AMENITIES_REQUIRED = "Amenities list is required";
    public static final String ROOM_STATUS_REQUIRED = "Room status is required";

    // Date validations
    public static final String DATE_START_REQUIRED = "Start date is required when filtering by availability";
    public static final String DATE_START_FUTURE = "Start date must be in present or future";
    public static final String DATE_END_REQUIRED = "End date is required when filtering by availability";
    public static final String DATE_END_FUTURE = "End date must be in the future";
    public static final String DATE_RANGE_INVALID = "End date must be after start date";

    // Capacity validations
    public static final String CAPACITY_MIN = "Minimum capacity is {min} person";
    public static final String CAPACITY_MAX = "Maximum allowed capacity is {max} people";

    // Price validations
    public static final String PRICE_NON_NEGATIVE = "Price cannot be negative";
    public static final String PRICE_RANGE_INVALID = "Maximum price must be greater than or equal to minimum price";

    // Sorting validations
    public static final String SORT_FIELD_REQUIRED = "Sort field cannot be empty";
    public static final String SORT_FIELD_INVALID = "Invalid sort criteria. Allowed values: {allowed}";
    public static final String ORDER_DIRECTION_REQUIRED = "Order direction cannot be empty";
    public static final String ORDER_DIRECTION_INVALID = "Invalid order direction. Use 'asc' or 'desc'";

    // Pagination validations
    public static final String PAGE_NON_NEGATIVE = "Page number cannot be negative";
    public static final String SIZE_MIN = "Minimum page size is {min}";
    public static final String SIZE_MAX = "Maximum page size is {max}";

    // Utility class pattern
    private ValidationMessages() {
        throw new IllegalStateException("Constants class");
    }
}
