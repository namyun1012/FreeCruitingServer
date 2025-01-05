package com.project.freecruting.controller;

import com.project.freecruting.dto.post.PostSaveRequestDto;
import com.project.freecruting.dto.post.PostUpdateRequestDto;
import com.project.freecruting.model.Post;
import com.project.freecruting.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostApiControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostRepository postRepository;

    @AfterEach
    public void tearDown() throws Exception {
        postRepository.deleteAll();
    }

    @Test
    public void register_post() throws Exception {
        String title = "title";
        String content = "content";
        String type = "type";
        PostSaveRequestDto requestDto = PostSaveRequestDto.builder()
                .title(title)
                .content(content)
                .author("author")
                .type(type)
                .build();


        String url = "http://localhost:" + port + "/api/v1/post";

        ResponseEntity<Long> responseEntity = restTemplate.postForEntity(url, requestDto, Long.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isGreaterThan(0L);
        List<Post> all = postRepository.findAll();
        assertThat(all.get(0).getTitle()).isEqualTo(title);
        assertThat(all.get(0).getContent()).isEqualTo(content);
        assertThat(all.get(0).getType()).isEqualTo(type);
    }

    @Test
    public void update_post() throws Exception {

        Post savedPost = postRepository.save(Post.builder()
                .title("title")
                .content("content")
                .author("author")
                .imageURL("temp")
                .type("type")
                .build());

        Long updateId = savedPost.getId();
        String title = "title2";
        String content = "content2";

        PostUpdateRequestDto requestDto = PostUpdateRequestDto.builder()
                .title(title)
                .content(content)
                .type("type")
                .build();
        // type은 Not Null 이어서 필수로 해야 하는 듯 하다.


        String url = "http://localhost:" + port + "/api/v1/post/" + updateId;

        HttpEntity<PostUpdateRequestDto> requestEntity = new HttpEntity<>(requestDto);

        ResponseEntity<Long> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Long.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isGreaterThan(0L);
    }
}
