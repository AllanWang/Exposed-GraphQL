package ca.allanwang.exposed.graphql

import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Created by Allan Wang on 2018-09-09.
 */
class EntityProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {

        val outputDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]?.run { File(this) }
                ?: run {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated kotlin files; use kapt")
                    return false
                }

        outputDir.mkdirs()
        File(outputDir, "asdf.kt").apply {
            createNewFile()
            writeText("Hello")
        }

        val elements = roundEnv.getElementsAnnotatedWith(GraphQLEntity::class.java)
                .filterIsInstance(TypeElement::class.java)

        if (elements.isEmpty()) return false

        val supplier = TypeSpec.classBuilder("TestSupplier")
                .addModifiers(KModifier.PROTECTED)

        elements.forEach { element ->
            val name = element.simpleName.toString()
            val packageName = element.packageName.toString()
            supplier.addFunction(FunSpec.builder("get$name")
                    .returns(ClassName(packageName, name))
                    .build())
        }

        val contents = FileSpec.builder("ca.allanwang.test", "TestSupplier")
                .addType(supplier.build()).build()

        println("Generated:\n\n$contents")
        contents.writeTo(outputDir)
        return true
    }

    private val Element.packageName: Name get() = processingEnv.elementUtils.getPackageOf(this).qualifiedName

    override fun getSupportedAnnotationTypes(): Set<String> =
            arrayOf(GraphQLEntity::class).map { it.java.canonicalName }.toSet()

    override fun getSupportedSourceVersion(): SourceVersion =
            SourceVersion.latestSupported()

    override fun getSupportedOptions(): Set<String> =
            setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)
}