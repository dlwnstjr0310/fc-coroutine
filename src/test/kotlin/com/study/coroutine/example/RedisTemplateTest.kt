package com.study.coroutine.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Range
import org.springframework.data.geo.Circle
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation
import org.springframework.data.redis.core.ReactiveListOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveZSetOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

@SpringBootTest
@ActiveProfiles("test")
class RedisTemplateTest(
    private val template: ReactiveRedisTemplate<Any, Any>
) : WithRedisContainer, StringSpec({

    val key = "key"

    afterTest {
        template.delete(key).awaitSingle()
    }

    "hello reactive redis" {

        val ops = template.opsForValue()

        shouldThrow<NoSuchElementException> {
            ops.get(key).awaitSingle()
        }

        ops.set(key, "blabla fc").awaitSingle()
        ops.get(key).awaitSingle() shouldBe "blabla fc"

        template.expire(key, 3.seconds.toJavaDuration()).awaitSingle()

        delay(5.seconds)

        shouldThrow<NoSuchElementException> {
            ops.get(key).awaitSingle()
        }
    }

    "LinkedList" {

        val ops = template.opsForList()

        ops.rightPushAll(key, 2, 3, 4, 5).awaitSingle()

        template.type(key).awaitSingle() shouldBe DataType.LIST

        ops.size(key).awaitSingle() shouldBe 4

//        for (i in 0 until ops.size(key).awaitSingle()) {
//            ops.index(key, i).awaitSingle().let {
//                logger.debug { "$i: $it" }
//            }
//        }

//        ops.range(key, 0, -1).asFlow().collect { logger.debug { it } }
//        ops.range(key, 0, -1).toStream().forEach { logger.debug { it } }
//        ops.range(key, 0, -1).asFlow().toList() shouldBe listOf(2, 3, 4, 5)

        ops.all(key) shouldBe listOf(2, 3, 4, 5)

        ops.rightPush(key, 6).awaitSingle()
        ops.all(key) shouldBe listOf(2, 3, 4, 5, 6)

        ops.leftPop(key).awaitSingle() shouldBe 2
        ops.all(key) shouldBe listOf(3, 4, 5, 6)

        ops.leftPush(key, 9).awaitSingle()
        ops.rightPop(key).awaitSingle() shouldBe 6
        ops.all(key) shouldBe listOf(9, 3, 4, 5)
    }

    "LinkedList LRU" {

        val ops = template.opsForList()

        ops.rightPushAll(key, 7, 6, 4, 3, 2, 1, 3).awaitSingle()

        ops.remove(key, 0, 2).awaitSingle()
        ops.all(key) shouldBe listOf(7, 6, 4, 3, 1, 3)

        ops.leftPush(key, 2).awaitSingle()
        ops.all(key) shouldBe listOf(2, 7, 6, 4, 3, 1, 3)
    }

    "Hash" {

        val ops = template.opsForHash<Int, String>()

        val map = (1..10).map { it to "val-$it" }.toMap()

        ops.putAll(key, map).awaitSingle()

        ops.size(key).awaitSingle() shouldBe 10

        ops.get(key, 1).awaitSingle() shouldBe "val-1"
        ops.get(key, 8).awaitSingle() shouldBe "val-8"

    }

    "Sorted Set" {

        val ops = template.opsForZSet()

        listOf(8, 7, 1, 4, 13, 22, 9, 7, 8).forEach {
            ops.add(key, "$it", -1.0 * Date().time).awaitSingle()
            ops.all(key).let { logger.debug { it } }
        }

        template.delete(key).awaitSingle()

        listOf(
            "jake" to 123,
            "chulsoo" to 752,
            "yeonghee" to 932,
            "john" to 335,
            "jake" to 623,
        ).also {
            it.toMap().toList().sortedBy { it.second }.let { logger.debug { "original : $it" } }
        }.forEach {
            ops.add(key, it.first, it.second * 1.0).awaitSingle()
            ops.all(key).let { logger.debug { it } }
        }

    }

    "Geo Redis" {

        val ops = template.opsForGeo()

        listOf(
            GeoLocation("seoul", Point(126.97806, 37.56667)),
            GeoLocation("busan", Point(129.07556, 35.17944)),
            GeoLocation("incheon", Point(126.70528, 37.45639)),
            GeoLocation("daegu", Point(128.60250, 35.87222)),
            GeoLocation("anyang", Point(126.95556, 37.39444)),
            GeoLocation("daejeon", Point(127.38500, 36.35111)),
            GeoLocation("gwangju", Point(126.85306, 35.15972)),
            GeoLocation("suwon", Point(127.02861, 37.26389)),
        ).forEach {
            ops.add(key, it as GeoLocation<Any>).awaitSingle()
        }

        ops.distance(key, "seoul", "busan").awaitSingle().let {
            logger.debug { "seoul -> busan : $it" }
        }

        val p = ops.position(key, "daegu").awaitSingle().also {
            logger.debug { it }
        }
        val circle = Circle(p, Distance(200.0, Metrics.KILOMETERS))

        ops.radius(key, circle).asFlow().map { it.content.name }.toList().let {
            logger.debug { "cities near daegu: $it" }
        }
    }

    "Hyper LogLog" {

        val ops = template.opsForHyperLogLog()

        ops.add("page1", "192.179.0.23", "41.61.2.230", "225.105.161.131").awaitSingle()
        ops.add("page2", "1.1.1.1", "2.2.2.2").awaitSingle()
        ops.add("page3", "9.9.9.9").awaitSingle()
        ops.add("page3", "8.8.8.8").awaitSingle()
        ops.add("page3", "7.7.7.7", "2.2.2.2", "1.1.1.1").awaitSingle()

        ops.size("page3").awaitSingle().let { logger.debug { it } }
    }

    "Redis Pub/Sub" {

        template.listenToChannel("channel-1").doOnNext {
            logger.debug { ">> received 1 : ${it.message}" }
        }.subscribe()

        template.listenToChannel("channel-1").doOnNext {
            logger.debug { ">> received 2 : ${it.message}" }
        }.subscribe()

        template.listenToChannel("channel-1").asFlow().onEach {
            logger.debug { ">> received 3 : ${it.message}" }
        }.launchIn(CoroutineScope(Dispatchers.Default))


        repeat(10) {
            val message = "test message (${it + 1})"
            logger.debug { ">> send : $message" }
            template.convertAndSend("channel-1", message).awaitSingle()
            delay(100)
        }

    }
})

interface WithRedisContainer {
    companion object {
        private val container = GenericContainer(DockerImageName.parse("redis")).apply {
            addExposedPorts(6379)
            start()
        }

        // companion object 가 static 과 동일한 기능을 하지만, static 과는 분명히 다르기에 속성을 명확히 해줌
        @JvmStatic
        @DynamicPropertySource
        fun setProperty(registry: DynamicPropertyRegistry) {
            logger.debug { "redis mapped port : ${container.getMappedPort(6379)}" }
            registry.add("spring.data.redis.port") {
                "${container.getMappedPort(6379)}"
            }
        }
    }
}

suspend fun ReactiveListOperations<Any, Any>.all(key: Any): List<Any> {
    return this.range(key, 0, -1).asFlow().toList()
}

suspend fun ReactiveZSetOperations<Any, Any>.all(key: Any): List<Any> {
    return this.range(key, Range.closed(0, -1)).asFlow().toList()
}