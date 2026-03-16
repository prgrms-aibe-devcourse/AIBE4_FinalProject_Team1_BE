package kr.inventory.domain.store.permission;

import kr.inventory.domain.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StorePermissionAspect {

    private static final String STORE_PUBLIC_ID_PARAMETER_NAME = "storePublicId";

    private final StorePermissionService storePermissionService;

    @Around("@annotation(requirePermission)")
    public Object validatePermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Long userId = extractAuthenticatedUserId();
        UUID storePublicId = extractStorePublicId(joinPoint);

        log.debug("[PermissionAspect] Validating - userId: {}, storePublicId: {}, permission: {}",
                userId, storePublicId, requirePermission.value());

        storePermissionService.validatePermission(userId, storePublicId, requirePermission.value());

        return joinPoint.proceed();
    }

    private Long extractAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new PermissionException(PermissionErrorCode.PERMISSION_DENIED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        throw new PermissionException(PermissionErrorCode.PERMISSION_CONTEXT_INVALID);
    }

    private UUID extractStorePublicId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (!(args[i] instanceof UUID uuidValue)) {
                continue;
            }

            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof PathVariable pathVariable && isStorePublicIdPathVariable(pathVariable, parameters[i])) {
                    return uuidValue;
                }
            }
        }

        throw new PermissionException(PermissionErrorCode.PERMISSION_CONTEXT_INVALID);
    }

    private boolean isStorePublicIdPathVariable(PathVariable pathVariable, Parameter parameter) {
        if (STORE_PUBLIC_ID_PARAMETER_NAME.equals(pathVariable.value())) {
            return true;
        }
        if (STORE_PUBLIC_ID_PARAMETER_NAME.equals(pathVariable.name())) {
            return true;
        }
        return STORE_PUBLIC_ID_PARAMETER_NAME.equals(parameter.getName());
    }
}
