package ca.allanwang.exposed.graphql

import com.squareup.kotlinpoet.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

/**
 * Created by Allan Wang on 2018-09-09.
 */
class EntityProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {

        val elements = roundEnv.getElementsAnnotatedWith(GraphQLEntity::class.java)
                .filterIsInstance(TypeElement::class.java)

        if (elements.isEmpty()) return true

        val supplier = TypeSpec.classBuilder("TestSupplier")
                .addModifiers(KModifier.PROTECTED)

        elements.forEach { element ->
            val name = element.simpleName.toString()
            val packageName = element.packageName.toString()
            supplier.addFunction(FunSpec.builder("get$name")
                    .returns(ClassName(packageName, name))
                    .build())
        }

        val file = processingEnv.filer.createSourceFile("ca.allanwang.test.TestSupplier")
        val contents = FileSpec.builder("ca.allanwang.test", "TestSupplier")
                .addType(supplier.build()).build()

        println("Generated:\n\n$contents")
        contents.writeTo(file.openWriter())
        return true
    }

    private val Element.packageName: Name get() = processingEnv.elementUtils.getPackageOf(this).qualifiedName

    override fun getSupportedAnnotationTypes(): Set<String> =
            arrayOf(GraphQLEntity::class).map { it.java.canonicalName }.toSet()

    override fun getSupportedSourceVersion(): SourceVersion =
            SourceVersion.latestSupported()
}