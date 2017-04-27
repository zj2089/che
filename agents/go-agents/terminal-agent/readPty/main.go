package main

import (
	"os"
	"log"
	"io"
	"os/exec"
	"github.com/eclipse/che-lib/pty"
	"io/ioutil"
	"fmt"
	"time"
)

type wsPty struct {
	id      int
	cmd     *exec.Cmd // pty builds on os.exec
	ptyFile *os.File  // a pty is simply an os.File
	Rdr     *io.ReadCloser
}

func startPty(command string) (*wsPty, error) {
	cmd := exec.Command(command)
	cmd.Env = append(os.Environ(), "TERM=xterm")

	file, err := pty.Start(cmd)
	if err != nil {
		return nil, err
	}

	//Set the size of the pty
	if err := pty.Setsize(file, 60, 200); err != nil {
		log.Printf("Error occurs on setting terminal size. %s", err)
	}

	return &wsPty{
		ptyFile: file,
		cmd:     cmd,
	}, nil
}

func main()  {
	wsPty, err := startPty("/bin/bash")
	if err != nil {
		log.Fatal("Failed to start pty process")
	}

	reader := ioutil.NopCloser(wsPty.ptyFile)

	done := make(chan bool)

	fmt.Println("File name : ", wsPty.ptyFile.Name())

	go readPty(reader, done)

	go func(done chan bool) {
		defer func() {done <- true}()

		timer := time.NewTicker(5 *time.Second)
		<- timer.C

		reader.Close()
	}(done)


	<- done
	fmt.Println("Application complete task and we try to read pty file again")

	newReader := ioutil.NopCloser(wsPty.ptyFile)
	go readPty(newReader, done)

	<- done

	fmt.Println("Application complete working")
}

func readPty(reader io.ReadCloser, done chan bool)  {
	defer func() {done <- true}()
	nBytes, nChunks := int64(0), int64(0)

	buf := make([]byte, 0, 1)

	for {
		n, err := reader.Read(buf[:cap(buf)])
		buf = buf[:n]
		if n == 0 {
			if err == nil {
				continue
			}
			if err == io.EOF {
				break
			}
			log.Fatal(err)
		}
		nChunks++
		nBytes += int64(len(buf))
		// process buf
		if err != nil && err != io.EOF {
			log.Fatal(err)
		}

		timer := time.NewTimer(time.Second)
		<- timer.C

		fmt.Println("Bytes:", nBytes, "Chunks:", nChunks, "Content : ", string(buf))
	}
}
