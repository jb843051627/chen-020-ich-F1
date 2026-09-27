package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIchImageCard;
import com.fc.v2.service.ITIchImageCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 技艺影像卷立卷单 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "技艺影像卷立卷单")
@Controller
@RequestMapping("/IchImageCardController")
public class IchImageCardController extends BaseController {

    private final String prefix = "admin/ichImageCard";

    @Autowired
    private ITIchImageCardService ichImageCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("ich:ichImageCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "技艺影像卷立卷单集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("ich:ichImageCard:list")
    @ResponseBody
    public ResultTable list(TIchImageCard record) {
        QueryWrapper<TIchImageCard> queryWrapper = new QueryWrapper<TIchImageCard>();
        startPage();
        com.github.pagehelper.PageInfo<TIchImageCard> page =
                new com.github.pagehelper.PageInfo<TIchImageCard>(ichImageCardService.selectTIchImageCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "技艺影像卷立卷单新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("ich:ichImageCard:add")
    @ResponseBody
    public AjaxResult add(TIchImageCard record) {
        return toAjax(ichImageCardService.insertTIchImageCard(record));
    }

    @Log(title = "技艺影像卷立卷单修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("ich:ichImageCard:edit")
    @ResponseBody
    public AjaxResult editSave(TIchImageCard record) {
        return toAjax(ichImageCardService.updateTIchImageCard(record));
    }

    @Log(title = "技艺影像卷立卷单删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("ich:ichImageCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(ichImageCardService.deleteTIchImageCardByIds(ids));
    }
}
