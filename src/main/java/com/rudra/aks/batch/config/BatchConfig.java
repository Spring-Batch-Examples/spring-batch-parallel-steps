package com.rudra.aks.batch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.rudra.aks.batch.model.UserBO;
import com.rudra.aks.batch.processor.UserItemProcessor;
import com.rudra.aks.batch.tasklet.Step2Tasklet;
import com.rudra.aks.batch.tasklet.Step3Tasklet;

@Configuration
@Import({DBConfig.class})
@EnableBatchProcessing
public class BatchConfig {

	@Autowired
	DataSource	dataSource;
    
    @Autowired
    private JobBuilderFactory jobs;
 
    @Autowired
    private StepBuilderFactory steps;
 

    /**
     * ItemReader to be used in chunk processing
     * 
     * @return	a FlatFileItemReader to read context from txt file and parse into UserBO
     * @throws  UnexpectedInputException
     * 			ParseException
     */
    @Bean
    public ItemReader<UserBO> itemReader() throws UnexpectedInputException, ParseException {

    	FlatFileItemReader<UserBO> reader = new FlatFileItemReader<UserBO>();
    	//reader.setLinesToSkip(1);
    	reader.setResource(new ClassPathResource("/record.txt"));
    	
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        String[] tokens = { "userid", "username", "emailid" };
        tokenizer.setNames(tokens);
        tokenizer.setDelimiter(",");
        
        BeanWrapperFieldSetMapper<UserBO> fieldSetMapper = new BeanWrapperFieldSetMapper<UserBO>();
        fieldSetMapper.setTargetType(UserBO.class);
        
        DefaultLineMapper<UserBO> lineMapper = new DefaultLineMapper<UserBO>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        reader.setLineMapper(lineMapper);
        reader.setSaveState(false);
        return reader;
    }
 
    /**
     * Simple ItemProcessor implementation
     * used for processing input dat before writing to db.
     * 
     * @return  same object, no processing
     */
    @Bean
    public ItemProcessor<UserBO, UserBO> itemProcessor() {
        return new UserItemProcessor();
    }
 
    /**
     * Item writer implementation using 
     * JdbcBatchItemWriter to write txt data into db.
     * 
     * 
     * @return  an item writer
     */
    @Bean
    public ItemWriter<UserBO>	dbItemWriter() {
    	JdbcBatchItemWriter<UserBO> dbWriter = new JdbcBatchItemWriter<UserBO>();
    	dbWriter.setDataSource(dataSource);
    	dbWriter.setSql("insert into USER_BATCH(userid, username, emailid) values (:userid, :username, :emailid)");
    	dbWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<UserBO>());
    	return dbWriter;
    }
    
    /**
     * Step creating with defined reader, processor & writer
     * {@link #itemReader()}, {@link #itemProcessor()} & {@link #dbItemWriter()}
     * 
     * @param   reader
     * 			processor
     * 			writer
     * @return  a single step to be executed as first step of the Job.
     */
    @Bean
    protected Step step1(ItemReader<UserBO> reader, ItemProcessor<UserBO, UserBO> processor, ItemWriter<UserBO> writer) {
        return steps.get("step1")
        			.<UserBO, UserBO> chunk(5)
        			.reader(reader)
        			.processor(processor)
        			.writer(writer)
        			.build();
    }
    
    @Bean(name = "tasklet2")
    protected Step step2() {
        return steps.get("tasklet2").tasklet(new Step2Tasklet()).build();
    }
    
    @Bean(name = "tasklet3")
    protected Step step3() {
        return steps.get("tasklet3").tasklet(new Step3Tasklet()).build();
    }
    
    @Bean
    protected Step step4(ItemReader<UserBO> reader, ItemProcessor<UserBO, UserBO> processor, ItemWriter<UserBO> writer) {
        return steps.get("step4")
        			.<UserBO, UserBO> chunk(10)
        			.reader(reader)
        			.processor(processor)
        			.writer(writer)
        			.build();
    }
 
    @Bean(name = "parallelsteps")
    public	Job	parallelJob() {
    	return  jobs.get("parallelsteps")
    				.incrementer(new RunIdIncrementer())
    				.start(step1(itemReader(), itemProcessor(), dbItemWriter()))
    				.split(taskExecutor()).add(flowBuilder())
    				.next(step4(itemReader(), itemProcessor(), dbItemWriter()))
    				.end().build();

    }
   
	private Flow	flowBuilder() {
    	final Flow	flow1 = new FlowBuilder<Flow>("flowto23").from(step2()).next(step3()).end();
    	return flow1;
    }

	@Bean
    public	TaskExecutor	taskExecutor() {
    	
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(2);
		taskExecutor.setMaxPoolSize(3);
    	taskExecutor.setThreadNamePrefix("job-thread");
    	return taskExecutor;
    }
}
