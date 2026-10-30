package Technical_Officer;

import Common.Session;

final class TechnicalOfficerContext {
    static final String DEPARTMENT_SQL =
            "(SELECT department_id FROM users WHERE user_id = ?)";
    private TechnicalOfficerContext() {}

    static int userId() {
        return Session.getUserId();
    }
}
