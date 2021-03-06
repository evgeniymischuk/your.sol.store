package controller;

import db.CacheDb;
import dto.ItemDto;
import dto.OrderDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import service.ItemService;
import service.OrderService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static service.CacheService.refreshCache;
import static service.FileService.save;
import static service.ItemService.*;
import static utils.CommonUtil.getId;

@Controller
public class AdminController {

    @RequestMapping("/admin")
    public String admin(Model model) throws Exception {
        refreshCache();
        model.addAttribute("itemList", CacheDb.itemList);
        return "admin";
    }

    @RequestMapping("/admin/add")
    @PostMapping
    public String add(
            ItemDto dto,
            @RequestParam(name = "small-image") MultipartFile smallImage,
            @RequestParam(name = "full-image") MultipartFile fullImage
    ) throws Exception {
        dto.setId(getId());
        final File savedSmallImage = save(dto.getId() + "_small.jpg", smallImage.getBytes());
        final File savedFullImage = save(dto.getId() + ".jpg", fullImage.getBytes());

        if ((savedFullImage == null || !savedFullImage.exists()) || (savedSmallImage == null || !savedSmallImage.exists())) {
            return "redirect:/admin";
        }

        final CSVFormat csvFormat = CacheDb.itemList.size() == 0 ? CSVFormat.DEFAULT.withHeader(ITEM_HEADER) : CSVFormat.DEFAULT;

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(ITEMS_CSV, true), csvFormat)) {
            printer.printRecord(
                    dto.getId(),
                    dto.getTitle(),
                    dto.getPrice(),
                    dto.getDescription(),
                    dto.getInstagramLikeUrl(),
                    dto.getReservation(),
                    "false"
            );
        }

        return "redirect:/admin";
    }

    @RequestMapping("/admin/remove")
    @PostMapping
    public String remove(ItemDto dto) throws IOException {

        removeOrReservation(Collections.singletonList(dto.getId()), false);

        return "redirect:/admin";
    }

    @RequestMapping("/admin/orders")
    public String orders(Model model) throws IOException {
        for (OrderDto orderDto : CacheDb.orderList) {
            List<ItemDto> purchasesDtoList = orderDto.getPurchasesDtoList();
            if (purchasesDtoList.remove(null)) {
                purchasesDtoList.clear();
                purchasesDtoList.addAll(findById(orderDto.getPurchasesIds()));
            }
        }
        model.addAttribute("orderList", CacheDb.orderList);
        return "orders";
    }

    @RequestMapping("/admin/orders/refresh")
    @PostMapping
    public @ResponseBody
    String ordersRefresh(Model model, @RequestParam String uid, ItemDto itemDto, OrderDto orderDto) throws IOException {
        itemDto.setId(uid);
        orderDto.setId(uid);
        ItemService.refresh(itemDto);
        OrderService.refresh(orderDto);
        return "done";
    }
}