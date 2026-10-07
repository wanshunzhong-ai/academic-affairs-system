import { reactive, ref, computed } from 'vue'

/**
 * 列表页通用逻辑：分页查询 / 加载 / 重置 / 多选
 *
 * @param {Function} fetcher 接收 query 对象、返回 Promise<Result> 的请求函数
 * @param {Object}   initialQuery 查询条件默认值（reset 时会恢复）
 * @param {Object}   options { immediate: 是否立即加载, pageSize: 每页条数 }
 */
export function useTable(fetcher, initialQuery = {}, options = {}) {
  const { immediate = false, pageSize = 10 } = options

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const selection = ref([])
  const query = reactive({ pageNum: 1, pageSize, ...initialQuery })

  const hasSelection = computed(() => selection.value.length > 0)
  const selectedIds = computed(() => selection.value.map((r) => r.id))

  async function load() {
    loading.value = true
    try {
      const res = await fetcher({ ...query })
      const data = res?.data
      if (Array.isArray(data)) {
        list.value = data
        total.value = data.length
      } else {
        list.value = data?.records || []
        total.value = Number(data?.total || 0)
      }
    } catch {
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
    return list.value
  }

  /** 条件查询：回到第一页 */
  function search() {
    query.pageNum = 1
    return load()
  }

  /** 保持当前页刷新 */
  function refresh() {
    return load()
  }

  /** 重置查询条件 */
  function reset(extra = {}) {
    Object.keys(query).forEach((k) => {
      if (k !== 'pageNum' && k !== 'pageSize') delete query[k]
    })
    Object.assign(query, { pageNum: 1, pageSize }, initialQuery, extra)
    selection.value = []
    return load()
  }

  function onSelectionChange(rows) {
    selection.value = rows || []
  }

  function onPageChange(page) {
    query.pageNum = page
    return load()
  }

  function onSizeChange(size) {
    query.pageSize = size
    query.pageNum = 1
    return load()
  }

  /** 删除后刷新：若当前页已空则回退一页 */
  async function refreshAfterRemove(removedCount = 1) {
    if (list.value.length <= removedCount && query.pageNum > 1) {
      query.pageNum -= 1
    }
    return load()
  }

  if (immediate) load()

  return {
    loading,
    list,
    total,
    query,
    selection,
    hasSelection,
    selectedIds,
    load,
    search,
    refresh,
    reset,
    onSelectionChange,
    onPageChange,
    onSizeChange,
    refreshAfterRemove
  }
}
