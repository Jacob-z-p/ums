package com.jacobzp.ums.exception;

import com.jacobzp.ums.model.Result;
import com.jacobzp.ums.model.ResultCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * <p>
 * 描述：全局异常处理器
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(AccountAlreadyExistsException.class) // 登录已被占用异常
    public ResponseEntity<Result<Void>> accountAlreadyExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.failure(ResultCode.ACCOUNT_EXISTS, "该登录账号已被占用"));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Result<Void> result;
        if (status.is5xxServerError()) {
            result = internalError(exception);
        } else {
            String description = switch (exception) {
                case HttpMessageNotReadableException ignored -> "请求体必须是有效的 JSON 对象";
                case MethodArgumentTypeMismatchException mismatch when "id".equals(mismatch.getName()) ->
                        "用户 ID 须为正整数";
                default -> "请检查请求格式与参数类型";
            };
            result = Result.failure(ResultCode.INVALID_PARAMETER, description);
        }
        return super.handleExceptionInternal(exception, result, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception exception) {
        return ResponseEntity.internalServerError().body(internalError(exception));
    }

    private Result<Void> internalError(Exception exception) {
        logger.error("业务接口处理失败", exception);
        return Result.failure(ResultCode.INTERNAL_ERROR, "请求暂时无法处理，请稍后重试");
    }
}
