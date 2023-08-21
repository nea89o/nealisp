package moe.nea.lisp.bind

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
annotation class LispBinding(
    val name: String,
)
