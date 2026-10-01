/**
 * 建档卡立卡/重录共用表单：
 * 方式一换，缺了便起不来的格子跟着换，用不上的空着（禁用不送）。
 * 卡号、版次、顺位、核验串不在表单内，服务层回填。
 */
layui.use(['form', 'jquery', 'layer'], function () {
    let form = layui.form;
    let $ = layui.jquery;
    let layer = layui.layer;
    let cfg = window.FLOG_FORM || {};

    // 按方式显隐专属栏；用不上的栏禁用，避免空值混送
    function applyMode(mode) {
        $('.mode-field').each(function () {
            let modes = ($(this).attr('data-modes') || '').split(',');
            let on = mode && modes.indexOf(String(mode)) >= 0;
            $(this).toggle(on);
            $(this).find('input,select,textarea').prop('disabled', !on);
        });
    }

    applyMode($('select[name="cutMode"]').val());
    form.on('select(cutMode)', function (data) {
        applyMode(data.value);
    });

    // 重录页预填
    if (cfg.prefill) {
        (function fill() {
            let p = cfg.prefill;
            $('select[name="siteId"]').val(p.siteId == null ? '' : String(p.siteId));
            ['subcompNo', 'cutBlockNo', 'qty', 'fineAmt', 'clearTrees', 'gradYears', 'gradInterval',
             'selectIntensity', 'selectKeepTrees', 'tendMethod', 'tendPerHa', 'renewMode',
             'renewFinishYear', 'reformTree', 'remark'].forEach(function (k) {
                if (p[k] != null) {
                    $('[name="' + k + '"]').val(p[k]);
                }
            });
            applyMode(p.cutMode);
            form.render(null, 'layui-form');
        })();
    }

    form.on('submit(' + (cfg.filterName || 'card-save') + ')', function (data) {
        // 被禁用的专属栏 layui 不会带，这里再兜底剔掉空串的非当前方式栏无须处理
        let loading = layer.load();
        $.ajax({
            url: cfg.url,
            data: data.field,
            dataType: 'json',
            type: 'post',
            success: function (r) {
                layer.close(loading);
                if (r.code === 200) {
                    let d = r.data || {};
                    layer.msg(r.msg + '｜卡号 ' + (d.billNo || '') + ' 第' + (d.nodeNo || '') +
                        '版，顺位 ' + (['', '轻度', '中度', '重度'][d.gradeLevel] || '') +
                        '，本卡占 ' + (d.occupyQty != null ? d.occupyQty : '') + 'm³，尚存 ' +
                        (d.remainQty != null ? d.remainQty : '') + 'm³',
                        {icon: 1, time: 2200}, function () {
                            parent.layer.close(parent.layer.getFrameIndex(window.name));
                            if (parent.layui && parent.layui.table) {
                                parent.layui.table.reload('card-table');
                            }
                        });
                } else {
                    // 缺哪格、哪张卡超额，服务层点名回执，原样亮出来
                    layer.msg(r.msg || '卡立不住', {icon: 2, time: 3000});
                }
            },
            error: function () {
                layer.close(loading);
                layer.msg('请求失败', {icon: 2});
            }
        });
        return false;
    });
});
