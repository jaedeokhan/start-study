package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("{id}")
    public UserPoint getPointById(
            @PathVariable long id
    ) {
        return pointService.getPointById(id);
    }

    @GetMapping("{id}/histories")
    public List<PointHistory> getHistoriesById(
            @PathVariable long id
    ) {
        return pointService.getHistoriesById(id);
    }

    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.charge(id, amount);
    }
    
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.use(id, amount);
    }
}
