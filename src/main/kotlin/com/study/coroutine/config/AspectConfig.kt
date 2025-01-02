package com.study.coroutine.config

import kotlinx.coroutines.slf4j.MDCContext
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.KotlinDetector
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation

@Aspect
@Component
class AspectConfig {

    @Around(
        """
        @annotation(org.springframework.web.bind.annotation.RequestMapping) ||
        @annotation(org.springframework.web.bind.annotation.GetMapping) ||
        @annotation(org.springframework.web.bind.annotation.PostMapping) ||
        @annotation(org.springframework.web.bind.annotation.PutMapping) ||
        @annotation(org.springframework.web.bind.annotation.PatchMapping) ||
        @annotation(org.springframework.web.bind.annotation.DeleteMapping)
    """
    )
    fun bindMdcContext(jp: ProceedingJoinPoint): Any? {
        return if (jp.hasSuspendFunction) {

            val continuation = jp.args.last() as Continuation<*>
            val newContext = continuation.context + MDCContext()
            val newContinuation = Continuation(newContext) { continuation.resumeWith(it) }

            val newArgs = jp.args.dropLast(1) + newContinuation

            jp.proceed(newArgs.toTypedArray())
        } else {
            jp.proceed()
        }
    }

    private val ProceedingJoinPoint.hasSuspendFunction: Boolean
        // jp.hasSuspendFunction 을 호출하게 되면 get() 이 호출됨
        // getter 가 아닌 타 로직 실행
        get() {
            val method = (this.signature as MethodSignature).method
            return KotlinDetector.isSuspendingFunction(method)
        }

}