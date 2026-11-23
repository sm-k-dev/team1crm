package com.team1.crm.customer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 고객 입력 Form의 필수 항목과 입력값 경계를 검증하는 테스트이다.
 * 전화번호 정규화·형식·중복 검사는 Service 테스트에서 별도로 확인한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-09
 */
class CustomerFormTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    /**
     * 모든 테스트에서 사용할 검증 도구를 준비한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * 테스트 완료 후 검증 도구의 자원을 정리한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    /**
     * 선택 항목을 입력하지 않은 정상 Form을 만든다.
     *
     * @return 이름과 전화번호만 입력한 Form
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    private CustomerForm validForm() {
        CustomerForm form = new CustomerForm();
        form.setName("김단골");
        form.setPhone("010-1234-5678");
        return form;
    }

    /**
     * 정상 입력과 선택 항목 미입력을 허용하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("정상 입력이면 생일 월·메모 미입력과 수신 미동의를 허용한다")
    void validInputPassesValidation() {
        CustomerForm form = validForm();

        assertTrue(validator.validate(form).isEmpty());
    }

    /**
     * 이름이 없거나 빈 문자열 또는 공백뿐이면 거부하는지 확인한다.
     *
     * @param name 검사할 이름
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("이름이 없거나 공백뿐이면 이름 검증에 실패한다")
    void blankNameFailsValidation(String name) {
        CustomerForm form = validForm();
        form.setName(name);

        assertTrue(
                validator.validateProperty(form, "name")
                        .stream()
                        .anyMatch(error -> error.getMessage()
                                .equals("고객 이름을 입력해 주세요."))
        );
    }

    /**
     * 이름 길이의 허용 경계인 1자와 30자를 허용하는지 확인한다.
     *
     * @param length 이름 길이
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    @DisplayName("이름이 1자 또는 30자이면 검증을 통과한다")
    void allowedNameLengthPassesValidation(int length) {
        CustomerForm form = validForm();
        form.setName("가".repeat(length));

        assertTrue(validator.validate(form).isEmpty());
    }

    /**
     * 이름이 최대 길이를 초과하면 거부하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("이름이 31자이면 이름 검증에 실패한다")
    void nameOver30CharactersFailsValidation() {
        CustomerForm form = validForm();
        form.setName("가".repeat(31));

        assertTrue(
                validator.validateProperty(form, "name")
                        .stream()
                        .anyMatch(error -> error.getMessage()
                                .equals("고객 이름은 30자 이내로 입력해 주세요."))
        );
    }

    /**
     * 전화번호가 없거나 빈 문자열 또는 공백뿐이면 거부하는지 확인한다.
     *
     * @param phone 검사할 전화번호
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("전화번호가 없거나 공백뿐이면 전화번호 검증에 실패한다")
    void blankPhoneFailsValidation(String phone) {
        CustomerForm form = validForm();
        form.setPhone(phone);

        assertTrue(
                validator.validateProperty(form, "phone")
                        .stream()
                        .anyMatch(error -> error.getMessage()
                                .equals("고객 전화번호를 입력해 주세요."))
        );
    }

    /**
     * 정규화 전에 사용하는 전화번호 표기를 Form에서 허용하는지 확인한다.
     * 실제 번호 형식의 유효성은 Service에서 검사한다.
     *
     * @param phone 검사할 전화번호
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "01012345678",
            "010-1234-5678",
            "010 1234 5678"
    })
    @DisplayName("전화번호의 숫자·하이픈·공백 표기를 Form에서 허용한다")
    void phoneInputFormatsPassFormValidation(String phone) {
        CustomerForm form = validForm();
        form.setPhone(phone);

        assertTrue(validator.validate(form).isEmpty());
    }

    /**
     * 생일 월의 허용 경계인 1월과 12월을 허용하는지 확인한다.
     *
     * @param month 검사할 생일 월
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 12})
    @DisplayName("생일 월이 1 또는 12이면 검증을 통과한다")
    void validBirthMonthPassesValidation(int month) {
        CustomerForm form = validForm();
        form.setBirthMonth(month);

        assertTrue(validator.validate(form).isEmpty());
    }

    /**
     * 생일 월이 허용 범위를 벗어나면 거부하는지 확인한다.
     *
     * @param month 검사할 생일 월
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 13})
    @DisplayName("생일 월이 0 또는 13이면 생일 월 검증에 실패한다")
    void invalidBirthMonthFailsValidation(int month) {
        CustomerForm form = validForm();
        form.setBirthMonth(month);

        assertTrue(
                validator.validateProperty(form, "birthMonth")
                        .stream()
                        .anyMatch(error -> error.getMessage()
                                .equals("생일 월은 1월부터 12월까지 선택해 주세요."))
        );
    }

    /**
     * 빈 메모와 공백 메모, 최대 길이의 메모를 허용하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("메모는 빈 문자열·공백·300자를 허용한다")
    void allowedMemoPassesValidation() {
        CustomerForm form = validForm();

        for (String memo : new String[]{"", "   ", "가".repeat(300)}) {
            form.setMemo(memo);

            assertTrue(
                    validator.validate(form).isEmpty(),
                    "허용되어야 하는 메모 길이: " + memo.length()
            );
        }
    }

    /**
     * 메모가 최대 길이를 초과하면 거부하는지 확인한다.
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @Test
    @DisplayName("메모가 301자이면 메모 검증에 실패한다")
    void memoOver300CharactersFailsValidation() {
        CustomerForm form = validForm();
        form.setMemo("가".repeat(301));

        assertTrue(
                validator.validateProperty(form, "memo")
                        .stream()
                        .anyMatch(error -> error.getMessage()
                                .equals("메모는 300자 이내로 입력해 주세요."))
        );
    }

    /**
     * 마케팅 수신 동의와 미동의를 모두 허용하는지 확인한다.
     *
     * @param agreed 마케팅 수신 동의 여부
     *
     * 작성자: 김순민
     * 작성일: 2026-11-09
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("마케팅 수신 동의와 미동의를 모두 허용한다")
    void marketingConsentPassesValidation(boolean agreed) {
        CustomerForm form = validForm();
        form.setAgreeMarketing(agreed);

        assertTrue(validator.validate(form).isEmpty());
    }
}