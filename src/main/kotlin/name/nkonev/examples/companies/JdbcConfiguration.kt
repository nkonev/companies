package name.nkonev.examples.companies

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import java.nio.ByteBuffer
import java.util.*


@EnableJdbcAuditing
@EntityScan(basePackages = ["name.nkonev.examples.companies"])
@EnableJdbcRepositories(basePackages = ["name.nkonev.examples.companies"])
@Configuration
class JdbcConfiguration(private val objectMapper: ObjectMapper) : AbstractJdbcConfiguration() {

    override fun userConverters(): List<Converter<*, *>> {
        return listOf(
            object : Converter<ByteArray, UUID> {
                override fun convert(source: ByteArray): UUID {
                    val byteBuffer: ByteBuffer = ByteBuffer.wrap(source)
                    val high: Long = byteBuffer.getLong()
                    val low: Long = byteBuffer.getLong()
                    return UUID(high, low)
                }
            },
            object : Converter<UUID, ByteArray> {
                override fun convert(source: UUID): ByteArray {
                    val bb = ByteBuffer.wrap(ByteArray(16))
                    bb.putLong(source.getMostSignificantBits())
                    bb.putLong(source.getLeastSignificantBits())
                    return bb.array()
                }
            },
            JsonNodeWritingConverter(objectMapper),
            JsonNodeReadingConverter(objectMapper),
        )
    }
}

internal abstract class AbstractJsonReadingConverter<T>(
    private val objectMapper: ObjectMapper,
    private val valueType: Class<T>
) : Converter<String, T> {

    override fun convert(source: String): T {
        return objectMapper.readValue(source, valueType)
    }
}

internal abstract class AbstractJsonWritingConverter<T> (
    private val objectMapper: ObjectMapper
) : Converter<T, String> {

    override fun convert(source: T): String {
        return objectMapper.writeValueAsString(source)
    }
}

internal class JsonNodeWritingConverter(objectMapper: ObjectMapper) : AbstractJsonWritingConverter<JsonNode>(objectMapper)


internal class JsonNodeReadingConverter(objectMapper: ObjectMapper) :
    AbstractJsonReadingConverter<JsonNode>(objectMapper, JsonNode::class.java)
