package guru.springframework.juniemvc.mappers;

import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderLine;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderLineResponse;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BeerOrderMapper {

    @Mapping(target = "lines", expression = "java(toLineResponses(entity.getLines()))")
    BeerOrderResponse toResponse(BeerOrder entity);

    @Mapping(target = "beerId", source = "beer.id")
    BeerOrderLineResponse toResponse(BeerOrderLine line);

    default List<BeerOrderLineResponse> toLineResponses(List<BeerOrderLine> lines) {
        return lines == null ? List.of() : lines.stream().map(this::toResponse).toList();
    }
}
