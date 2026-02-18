package xyz.robinjoon.growweek.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture

private const val BASE_PACKAGE = "xyz.robinjoon.growweek"
private val BOUNDED_CONTEXTS = listOf("member", "task", "retrospective")

@Suppress("ktlint:standard:property-naming")
@AnalyzeClasses(
    packages = [BASE_PACKAGE],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {
    /**
     * 계층 의존 방향 검증.
     *
     * - Presentation → Application 만 허용
     * - Application → Domain 만 허용
     * - Infrastructure → Domain 만 허용
     *
     * 예외:
     * - common 패키지는 공유 모듈이므로 레이어 규칙에서 제외
     * - Application DTO가 domain 타입(enum, value class)을 필드로 노출하므로
     *   Presentation이 DTO 변환 시 domain.model에 bytecode 의존이 발생 (아키텍처 문서 허용 패턴)
     */
    @ArchTest
    val `계층 의존 방향을 준수해야 한다`: ArchRule =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Presentation")
            .definedBy("..presentation..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Infrastructure")
            .definedBy("..infrastructure..")
            // common 패키지: 공유 모듈이므로 레이어 규칙에서 제외
            .ignoreDependency(
                JavaClass.Predicates.resideInAPackage("..common.."),
                DescribedPredicate.alwaysTrue(),
            ).ignoreDependency(
                DescribedPredicate.alwaysTrue(),
                JavaClass.Predicates.resideInAPackage("..common.."),
            )
            // Application DTO → domain 타입 노출에 따른 Presentation bytecode 의존 허용
            // (domain.service, domain.repository 의존은 여전히 금지)
            .ignoreDependency(
                JavaClass.Predicates.resideInAPackage("..presentation.."),
                JavaClass.Predicates.resideInAPackage("..domain.model.."),
            ).whereLayer("Presentation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()

    @ArchTest
    val `BC 간 직접 참조가 없어야 한다 - member`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE_PACKAGE.member..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                *BOUNDED_CONTEXTS
                    .filter { it != "member" }
                    .map { "$BASE_PACKAGE.$it.." }
                    .toTypedArray(),
            )

    @ArchTest
    val `BC 간 직접 참조가 없어야 한다 - task`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE_PACKAGE.task..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                *BOUNDED_CONTEXTS
                    .filter { it != "task" }
                    .map { "$BASE_PACKAGE.$it.." }
                    .toTypedArray(),
            )

    @ArchTest
    val `BC 간 직접 참조가 없어야 한다 - retrospective`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE_PACKAGE.retrospective..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                *BOUNDED_CONTEXTS
                    .filter { it != "retrospective" }
                    .map { "$BASE_PACKAGE.$it.." }
                    .toTypedArray(),
            )

    @ArchTest
    val `Domain 레이어는 Spring 프레임워크에 의존하지 않아야 한다`: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
            )
}
