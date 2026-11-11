<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="danger" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
h1 {
  font-size: 72px;
  margin: 0;
  color: #b42318;
}
</style>