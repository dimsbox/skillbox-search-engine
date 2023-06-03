package searchengine.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
public class ResultDTO {

    private boolean result;

    private String error;

    private int count;

    private HttpStatus status;

    private List<SearchDTO> data;

    public ResultDTO(boolean result) {
        this.result = result;
    }

    public ResultDTO(boolean result, HttpStatus status) {
        this.result = result;
        this.status = status;
    }

    public ResultDTO(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public ResultDTO(boolean result, String error, HttpStatus status) {
        this.result = result;
        this.error = error;
        this.status = status;
    }

    public ResultDTO(boolean result, int count, List<SearchDTO> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public ResultDTO(boolean result, int count, List<SearchDTO> data, HttpStatus status) {
        this.result = result;
        this.count = count;
        this.data = data;
        this.status = status;
    }
}

