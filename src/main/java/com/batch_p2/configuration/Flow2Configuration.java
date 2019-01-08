package com.batch_p2.configuration;

import com.batch_p2.model.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class Flow2Configuration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    DataSource dataSource;

    private Resource outputResource = new FileSystemResource("output/outputData.csv");

    @Bean
    public JdbcCursorItemReader<Customer> jdbcCursorItemReader() throws Exception{
        JdbcCursorItemReader<Customer> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(this.dataSource);
        reader.setFetchSize(10);
        reader.setRowMapper((resultSet, i) ->
                new Customer(resultSet.getLong("id"),
                resultSet.getString("firstName"),
                resultSet.getString("lastName"),
                resultSet.getDate("birthdate")));
        reader.setSql("select * from customer");

        return reader;
    }

    @Bean
    public FlatFileItemWriter<Customer> flatFileItemWriter() throws Exception {
        FlatFileItemWriter<Customer> itemWriter = new FlatFileItemWriter<>();

        itemWriter.setResource(outputResource);
        itemWriter.setAppendAllowed(true);
        DelimitedLineAggregator<Customer> aggregator = new DelimitedLineAggregator<>();
        aggregator.setDelimiter(DelimitedLineTokenizer.DELIMITER_COMMA);
        BeanWrapperFieldExtractor<Customer> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[]{"id", "firstName", "lastName", "birthdate"});
        aggregator.setFieldExtractor(extractor);
        itemWriter.setLineAggregator(aggregator);
        itemWriter.setHeaderCallback(writer -> writer.write("id,firstName,lastName,birthdate"));

        return itemWriter;
    }

//    @Bean
//    public CompositeItemWriter<Customer> itemWriter() throws Exception{
//        List<ItemWriter<? super Customer>> writers = new ArrayList<>(2);
//        writers.add(customerItemWriter());
//        writers.add(customerItemWriter());
//    }
    @Bean
    public Step step2() throws Exception {
        return stepBuilderFactory.get("step2")
                .<Customer, Customer>chunk(10)
                .reader(jdbcCursorItemReader())
                .writer(flatFileItemWriter())
                .build();
    }

    @Bean
    public Flow flow2() throws Exception{
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("flow2");

        flowBuilder.start(step2())
                .end();

        return flowBuilder.build();
    }
}
