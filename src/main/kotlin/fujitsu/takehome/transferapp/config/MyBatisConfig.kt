package fujitsu.takehome.transferapp.config

import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.annotation.MapperScan
import org.mybatis.spring.boot.autoconfigure.MybatisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@MapperScan(basePackages = ["fujitsu.takehome.transferapp.repository.mapper"])
class MyBatisConfig(
    private val dataSource: DataSource?,
    private val properties: MybatisProperties
) {

    @Bean
    fun sqlSessionFactory(): SqlSessionFactory? {
        val sqlSessionFactoryBean = SqlSessionFactoryBean()
        sqlSessionFactoryBean.setDataSource(dataSource)
        return sqlSessionFactoryBean.`object`
    }
}