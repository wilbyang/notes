

### 文本和正则
```
use regex::Regex;

lazy_static!{
    static ref REGEX: Regex = Regex::new(r"^John\d$").unwrap();
}

REGEX.is_match(&self.name)
```
### 测试
```
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn test_regex() {
        assert!(REGEX.is_match("John1"));
        assert!(!REGEX.is_match("John"));
    }
}
```
### 内存管理

### 数据序列化和错误处理
```
use std::io;
use serde::{Deserialize, Serialize};
use thiserror::Error;
mod mod1;
fn main() {

    let x = test();
    match x {
        Ok(_) => {}
        Err(DataStoreError::JSONError(e)) => {println!("JSONError: {}", e)}
        Err(DataStoreError::Redaction(e)) => {println!("RedactionError: {}", e)}
        _ => {}
    }

}
fn test() -> Result<(), DataStoreError> {
    // http get httpbin.org


    println!("Hello, world!");
    let my = MyStruct::new("John".to_string(), 32);
    println!("my: {:?}", my);
    let json = serde_json::to_string(&my)?;
    let s = serde_json::from_str::<MyStruct>(&json)?;
    println!("s: {:?}", s);
    if s.name != "John2" {
        return Err(DataStoreError::Redaction("name".to_string()));
    }
    Ok(())

}

#[derive(Debug, Serialize, Deserialize)]
struct MyStruct {
    name: String,
    age: u8,
}

impl MyStruct {
    fn new(name: String, age: u8) -> Self {
        Self {
            name,
            age,
        }
    }
}

#[derive(Error, Debug)]
pub enum DataStoreError {

    #[error("data store disconnected")]
    Disconnect(#[from] io::Error),
    #[error("malformed json")]
    JSONError(#[from] serde_json::Error),

    #[error("the data for key `{0}` is not available")]
    Redaction(String),

    #[error("invalid header (expected {expected:?}, found {found:?})")]
    InvalidHeader {
        expected: String,
        found: String,
    },

    #[error("unknown data store error")]
    Unknown,
}
```

### 抽象


