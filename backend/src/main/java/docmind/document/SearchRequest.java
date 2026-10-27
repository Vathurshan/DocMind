package docmind.document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(

        @NotBlank(message = "Query must not be blank")
        @Size(max = 2000, message = "Query must not exceed 2000 characters")
        String query,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 20, message = "Limit must not exceed 20")
        int limit
) {
    // Default limit to 10 when not supplied / 0
    public SearchRequest {
        if (limit <= 0) limit = 10;
    }
}
