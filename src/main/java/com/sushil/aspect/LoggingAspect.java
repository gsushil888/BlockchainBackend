package com.sushil.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.sushil.controller..*)")
    private void controllerLayer() {}

    @Pointcut("within(com.sushil.service..*)")
    private void serviceLayer() {}

    @Around("controllerLayer()")
    public Object logController(ProceedingJoinPoint pjp) throws Throwable {
        return profile(pjp, "CTRL");
    }

    @Around("serviceLayer()")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        return profile(pjp, "SVC");
    }

    private Object profile(ProceedingJoinPoint pjp, String layer) throws Throwable {
        MethodSignature sig    = (MethodSignature) pjp.getSignature();
        var             logger = LoggerFactory.getLogger(pjp.getTarget().getClass());
        String          method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String          args   = formatArgs(sig, pjp.getArgs());

        logger.info("[{}] → {}{}", layer, method, args);
        long start = System.currentTimeMillis();

        try {
            Object result  = pjp.proceed();
            long   elapsed = System.currentTimeMillis() - start;
            logger.info("[{}] ← {} ({}ms) → {}", layer, method, elapsed, summarize(result));
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            logger.error("[{}] ✗ {} ({}ms) — {}: {}", layer, method, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private static String formatArgs(MethodSignature sig, Object[] args) {
        if (args == null || args.length == 0) return "()";
        String[] names = sig.getParameterNames();
        String joined = IntStream.range(0, args.length)
                .mapToObj(i -> {
                    String name = (names != null && i < names.length) ? names[i] : "arg" + i;
                    return name + "=" + sanitize(name, args[i]);
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return "(" + joined + ")";
    }

    private static Object sanitize(String name, Object value) {
        if (name != null && (name.toLowerCase().contains("password") || name.toLowerCase().contains("otp"))) return "***";
        if (value == null) return "null";
        String str = value.toString();
        if (str.startsWith("eyJ") && str.length() > 20) return str.substring(0, 20) + "...[JWT]";
        return str;
    }

    private static String summarize(Object result) {
        if (result == null) return "void";
        String str = result.toString();
        return str.length() > 120 ? str.substring(0, 120) + "..." : str;
    }
}
